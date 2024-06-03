(ns pigeon-scoops.components.recipe-manager
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [honey.sql :as sql]
            [honey.sql.helpers :as hsql :refer [select
                                                from
                                                where
                                                delete-from
                                                insert-into
                                                values]]
            [next.jdbc :as jdbc])
  (:import (java.util UUID)))

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
         recipe-values (-> new-recipe
                           (dissoc ::rs/ingredients)
                           (update ::rs/amount-unit name)
                           (update ::rs/type name)
                           (update ::rs/instructions #(vec [:array % :text]))
                           (assoc ::rs/amount-unit-type (namespace (::rs/amount-unit new-recipe))
                                  ::rs/source (::rs/source new-recipe))
                           (update-keys (comp keyword name)))
         recipe-statement (if update?
                            (-> (hsql/update :recipes)
                                (hsql/set recipe-values)
                                (where [:= :id recipe-id])
                                sql/format)
                            (-> (insert-into :recipes)
                                (values [recipe-values])
                                sql/format))
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
             (jdbc/execute! conn
                            (-> (delete-from :ingredients)
                                (where [:= :recipe-id recipe-id])
                                sql/format))
             (jdbc/execute! conn
                            recipe-statement)
             (when-not (empty? (::rs/ingredients new-recipe))
               (jdbc/execute! conn
                              ingredients-statement))
             new-recipe))))))

(defrecord RecipeManager [database grocery-manager]
  component/Lifecycle

  (start [this]
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

(defn can-merge-recipe-ingredients? [recipe-ingredients]
  (and (= 1 (count (set (map ::rs/ingredient-type recipe-ingredients))))
       (every? #(some #{(namespace (::rs/amount-unit %))} (map namespace [::volume/l ::mass/g])) recipe-ingredients)))

(defn apply-grocery-unit [recipe-ingredient grocery-unit]
  (let [to-apply (cond (= (namespace (::rs/amount-unit recipe-ingredient))
                          (namespace ::mass/g))
                       (::gs/unit-mass-type grocery-unit)
                       (= (namespace (::rs/amount-unit recipe-ingredient))
                          (namespace ::volume/l))
                       (::gs/unit-volume-type grocery-unit)
                       :else
                       nil)]
    (if to-apply
      (-> recipe-ingredient
          (assoc ::rs/amount-unit to-apply)
          (update ::rs/amount units/convert (::rs/amount-unit recipe-ingredient) to-apply))
      recipe-ingredient)))

(defn change-unit-type [to-type recipe-ingredient conversion-unit]
  (if (or (= to-type (namespace (::rs/amount-unit recipe-ingredient)))
          (some false? (map (partial contains? conversion-unit) [::gs/unit-volume ::gs/unit-mass])))
    recipe-ingredient
    (-> recipe-ingredient
        (apply-grocery-unit conversion-unit)
        (update ::rs/amount-unit (cond (= to-type (namespace ::mass/g))
                                       (fn [_] (::gs/unit-mass-type conversion-unit))
                                       (= to-type (namespace ::volume/c))
                                       (fn [_] (::gs/unit-volume-type conversion-unit))
                                       :else
                                       identity))
        (update ::rs/amount (cond (= to-type (namespace ::mass/g))
                                  #(* % (/ (::gs/unit-mass conversion-unit)
                                           (::gs/unit-volume conversion-unit)))
                                  (= to-type (namespace ::volume/c))
                                  #(* % (/ (::gs/unit-volume conversion-unit)
                                           (::gs/unit-mass conversion-unit)))
                                  :else
                                  identity)))))

(defn merge-recipe-ingredients [recipes-to-merge groceries]
  (->> (mapcat ::rs/ingredients recipes-to-merge)
       (group-by ::rs/ingredient-type)
       vals
       (map (fn [recipe-ingredients]
              (if (or (= (count recipe-ingredients) 1)
                      (not (can-merge-recipe-ingredients? recipe-ingredients)))
                recipe-ingredients
                (let [to-type (namespace (::rs/amount-unit (first recipe-ingredients)))
                      ingredient-type (::rs/ingredient-type (first recipe-ingredients))
                      conversion-unit (first (filter #(and (some? (::gs/unit-volume %))
                                                           (some? (::gs/unit-mass %)))
                                                     (::gs/units (first (filter #(= (::gs/type %) ingredient-type)
                                                                                groceries)))))]
                  (map #(change-unit-type to-type % conversion-unit) recipe-ingredients)))))
       (apply concat)
       (group-by #(list (::rs/ingredient-type %) (namespace (::rs/amount-unit %))))
       vals
       (map #(reduce (fn [acc ingredient]
                       (update acc ::rs/amount + (units/convert (::rs/amount ingredient)
                                                                (::rs/amount-unit ingredient)
                                                                (::rs/amount-unit acc)))) %))))

(defn to-grocery-purchase-list [recipe-ingredients groceries]
  (let [grocery-map (into {} (map #(vec [(::gs/type %) %]) groceries))
        purchase-list (->> recipe-ingredients
                           (map #(gm/divide-grocery (::rs/amount %)
                                                    (::rs/amount-unit %)
                                                    ((::rs/ingredient-type %) grocery-map))))]
    {:purchase-list       purchase-list
     :total-purchase-cost (apply + (filter some? (map ::gs/purchase-cost purchase-list)))
     :total-needed-cost   (apply + (filter some? (map ::gs/amount-needed-cost purchase-list)))}))
