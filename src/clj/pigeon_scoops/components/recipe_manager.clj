(ns pigeon-scoops.components.recipe-manager
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [select
                                       from
                                       where
                                       delete-from
                                       insert-into
                                       values]]
            [next.jdbc :as jdbc])
  (:import (java.util UUID)))

(def create-recipe-table-statement {:create-table [:recipes :if-not-exists]
                                    :with-columns
                                    [[:id :uuid [:not nil] :primary-key]
                                     [:type :text [:not nil]]
                                     [:name :text [:not nil]]
                                     [:instructions :text :array]
                                     [:amount :real [:not nil]]
                                     [:amount-unit :text [:not nil]]
                                     [:amount-unit-type :text [:not nil]]
                                     [:source :text]]})

(def create-ingredient-table {:create-table [:ingredients :if-not-exists]
                              :with-columns
                              [[:recipe-id :uuid [:references :recipes :id] [:not nil]]
                               [:ingredient-type :text [:references :groceries :type] [:not nil]]
                               [:amount :real [:not nil]]
                               [:amount-unit :text [:not nil]]
                               [:amount-unit-type :text [:not nil]]
                               [:primary :key [:composite :recipe-id :ingredient-type]]]})

(defn ingredient-from-db [ingredient]
  (-> (db/from-db-namespace ::rs/ingredient ingredient)
      (dissoc ::rs/recipe-id ::rs/amount-unit-type)
      (update ::rs/amount-unit #(keyword (:ingredients/amount_unit_type ingredient) %))
      (update ::rs/ingredient-type #(keyword (namespace ::gs/entry) %))))

(defn from-db [recipes ingredients]
  (->> recipes
       (map (partial db/from-db-namespace ::rs/entry))
       (map (fn [recipe]
              (-> recipe
                  (assoc ::rs/ingredients (map ingredient-from-db
                                               (filter #(= (:ingredients/recipe_id %)
                                                           (::rs/id recipe)) ingredients)))
                  (dissoc ::rs/amount-unit-type)
                  (update ::rs/amount-unit #(keyword (::rs/amount-unit-type recipe) %))
                  (update ::rs/type #(keyword (namespace ::rs/entry) %)))))))

(defn get-recipes! [recipe-manager & ids]
  (let [recipes (jdbc/execute! (-> recipe-manager :database ::db/connection)
                               (cond-> (-> (select :*)
                                           (from :recipes))
                                       (not-empty ids) (where [:in :id ids])
                                       :then sql/format))
        ingredients (when (not-empty recipes)
                      (jdbc/execute! (-> recipe-manager :database ::db/connection)
                                     (-> (select :*)
                                         (from :ingredients)
                                         (where [:in :recipe-id (map :recipes/id recipes)])
                                         sql/format)))]
    (from-db recipes ingredients)))

(defn unsafe-delete-recipe! [conn id]
  (jdbc/execute! conn
                 (-> (delete-from :ingredients)
                     (where [:= :recipe-id id])
                     sql/format))
  (jdbc/execute! conn
                 (-> (delete-from :recipes)
                     (where [:= :id id])
                     sql/format)))

(defn delete-recipe! [recipe-manager id]
  (logger/info (str "Deleting " id))
  (jdbc/with-transaction [conn (-> recipe-manager :database ::db/connection)]
                         (unsafe-delete-recipe! conn id)))

(defn add-recipe!
  ([recipe-manager new-recipe]
   (add-recipe! recipe-manager new-recipe false))
  ([recipe-manager new-recipe update?]
   (logger/info (str "Adding " (::rs/name new-recipe)))
   (let [recipe-id (if update?
                     (::rs/id new-recipe)
                     (or (::rs/id new-recipe) (UUID/randomUUID)))
         existing (first (get-recipes! recipe-manager recipe-id))
         new-recipe (assoc new-recipe ::rs/id recipe-id)
         recipe-statement (-> (insert-into :recipes)
                              (values [(-> new-recipe
                                           (dissoc ::rs/ingredients)
                                           (update ::rs/amount-unit name)
                                           (update ::rs/type name)
                                           (update ::rs/instructions #(vec [:array % :text]))
                                           (assoc ::rs/amount-unit-type (namespace (::rs/amount-unit new-recipe)))
                                           (update-keys (comp keyword name)))])
                              sql/format)
         ingredients-statement (-> (insert-into :ingredients)
                                   (values (map #(conj {:recipe-id        recipe-id
                                                        :amount-unit-type (namespace (::rs/amount-unit %))}
                                                       (update-keys
                                                         (-> %
                                                             (update ::rs/amount-unit name)
                                                             (update ::rs/ingredient-type name))
                                                         (comp keyword name)))
                                                (::rs/ingredients new-recipe)))
                                   sql/format)]
     (or (s/explain-data ::rs/entry new-recipe)
         (when-not (or (and update? (not existing))
                       (and (not update?) existing))
           (jdbc/with-transaction
             [conn (-> recipe-manager :database ::db/connection)]
             (unsafe-delete-recipe! conn recipe-id)
             (jdbc/execute! conn
                            recipe-statement)
             (when-not (empty? (::rs/ingredients new-recipe))
               (jdbc/execute! conn
                              ingredients-statement))
             new-recipe))))))

(defrecord RecipeManager [database grocery-manager]
  component/Lifecycle

  (start [this]
    (jdbc/execute! (::db/connection database) (sql/format create-recipe-table-statement))
    (jdbc/execute! (::db/connection database) (sql/format create-ingredient-table))
    (->> "recipes.edn"
         io/resource
         slurp
         edn/read-string
         (map (partial add-recipe! this))
         doall)
    (assoc this ::recipe {}))

  (stop [this]
    (assoc this ::recipes nil)))

(defn make-recipe-manager []
  (map->RecipeManager {}))

(defn scale-recipe [recipe amount amount-unit]
  (let [scale-factor (units/scale-factor (::rs/amount recipe)
                                         (::rs/amount-unit recipe)
                                         amount
                                         amount-unit)]
    (-> recipe
        (update ::rs/ingredients (partial map #(update % ::rs/amount * scale-factor)))
        (assoc ::rs/amount amount
               ::rs/amount-unit amount-unit))))

(defn merge-recipe-ingredients [recipes-to-merge]
  (->> (mapcat ::rs/ingredients recipes-to-merge)
       (group-by #(list (::rs/ingredient-type %) (namespace (::rs/amount-unit %))))
       vals
       (map #(reduce (fn [acc ingredient]
                       (update acc ::rs/amount + (units/convert (::rs/amount ingredient)
                                                                (::rs/amount-unit ingredient)
                                                                (::rs/amount-unit acc)))) %))))

(defn to-grocery-purchase-list [recipe-ingredients groceries]
  (let [grocery-map (into {} (map #(vec [(::gs/type %) %]) groceries))
        purchase-list (map #(gm/divide-grocery (::rs/amount %)
                                               (::rs/amount-unit %)
                                               ((::rs/ingredient-type %) grocery-map))
                           recipe-ingredients)]
    {:purchase-list purchase-list
     :total-cost    (apply + (map #(* (::gs/unit-cost %) (::gs/unit-purchase-quantity %))
                                  (mapcat ::gs/units purchase-list)))}))
