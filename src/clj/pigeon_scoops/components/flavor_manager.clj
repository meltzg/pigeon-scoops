(ns pigeon-scoops.components.flavor-manager
  (:require [clojure.tools.logging :as logger]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [honey.sql.helpers :as hsql :refer [select
                                                from
                                                where
                                                delete-from
                                                insert-into
                                                values]]
            [next.jdbc :as jdbc]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as units])
  (:import (java.util UUID)))

(defn mixin-from-db [mixin]
  (-> (db/from-db-namespace ::fs/entry mixin)
      (dissoc ::fs/flavor-id ::fs/amount-unit-type)
      (update ::fs/amount-unit #(keyword (:mixins/amount_unit_type mixin) %))))

(defn from-db [flavors mixins]
  (->> flavors
       (map (partial db/from-db-namespace ::fs/entry))
       (map (fn [flavor]
              (-> flavor
                  (assoc ::fs/mixins (map mixin-from-db
                                          (filter #(= (:mixins/flavor_id %)
                                                      (::fs/id flavor)) mixins)))
                  (dissoc ::fs/amount-unit-type)
                  (update ::fs/amount-unit #(keyword (::fs/amount-unit-type flavor) %)))))))


(defn scale-flavor [flavor amount amount-unit]
  (let [scale-factor (units/scale-factor (::fs/amount flavor)
                                         (::fs/amount-unit flavor)
                                         amount
                                         amount-unit)]
    (-> flavor
        (update ::fs/mixins (partial map #(update % ::fs/amount * scale-factor)))
        (assoc ::fs/amount amount
               ::fs/amount-unit amount-unit))))

(defn get-flavors! [flavor-manager & ids]
  (let [flavors (jdbc/execute! (-> flavor-manager :database ::db/connection)
                               (cond-> (-> (select :*)
                                           (from :flavors))
                                       (not-empty ids) (where [:in :id ids])
                                       :then sql/format))
        mixins (when (not-empty flavors)
                 (jdbc/execute! (-> flavor-manager :database ::db/connection)
                                (-> (select :*)
                                    (from :mixins)
                                    (where [:in :flavor-id (map :flavors/id flavors)])
                                    sql/format)))]
    (from-db flavors mixins)))

(defn unsafe-delete-flavor! [conn id]
  (jdbc/execute! conn
                 (-> (delete-from :mixins)
                     (where [:= :flavor-id id])
                     sql/format))
  (jdbc/execute! conn
                 (-> (delete-from :flavors)
                     (where [:= :id id])
                     sql/format)))

(defn delete-flavor! [flavor-manager id]
  (logger/info (str "Deleting " id))
  (jdbc/with-transaction [conn (-> flavor-manager :database ::db/connection)]
                         (unsafe-delete-flavor! conn id)))

(defn add-flavor!
  ([flavor-manager new-flavor]
   (add-flavor! flavor-manager new-flavor false))
  ([flavor-manager new-flavor update?]
   (logger/info (str "Adding " (::fs/name new-flavor)))
   (let [flavor-id (if update?
                     (::fs/id new-flavor)
                     (or (::fs/id new-flavor) (UUID/randomUUID)))
         existing (first (get-flavors! flavor-manager flavor-id))
         new-flavor (assoc new-flavor ::fs/id flavor-id)
         flavor-values (-> new-flavor
                           (dissoc ::fs/mixins)
                           (update ::fs/amount-unit name)
                           (update ::fs/instructions #(vec [:array % :text]))
                           (assoc ::fs/amount-unit-type (namespace (::fs/amount-unit new-flavor)))
                           (update-keys (comp keyword name)))
         flavor-statement (if update?
                            (-> (hsql/update :flavors)
                                (hsql/set flavor-values)
                                (where [:= :id flavor-id]))
                            (-> (insert-into :flavors)
                                (values [flavor-values])))
         mixin-statement (-> (insert-into :mixins)
                             (values (map #(conj {:flavor-id        flavor-id
                                                  :amount-unit-type (namespace (::fs/amount-unit %))}
                                                 (update-keys
                                                   (update % ::fs/amount-unit name)
                                                   (comp keyword name)))
                                          (::fs/mixins new-flavor))))]
     (or (s/explain-data ::fs/entry new-flavor)
         (when-not (or (and update? (not existing))
                       (and (not update?) existing))
           (jdbc/with-transaction
             [conn (-> flavor-manager :database ::db/connection)]
             (jdbc/execute! conn
                            (-> (delete-from :mixins)
                                (where [:= :flavor-id flavor-id])
                                sql/format))
             (jdbc/execute! conn
                            (sql/format flavor-statement))
             (when-not (empty? (::fs/mixins new-flavor))
               (jdbc/execute! conn
                              (sql/format mixin-statement)))
             new-flavor))))))

(defn materialize-recipes! [flavor-manager flavor]
  (let [recipe-ids (concat [(::fs/recipe-id flavor)] (map ::fs/recipe-id (::fs/mixins flavor)))
        recipe-map (into {} (map #(vec [(::rs/id %) %])
                                 (apply (partial rm/get-recipes! (:recipe-manager flavor-manager))
                                        recipe-ids)))]
    (concat [(rm/scale-recipe (get recipe-map (::fs/recipe-id flavor))
                              (::fs/amount flavor)
                              (::fs/amount-unit flavor))]
            (map #(rm/scale-recipe (get recipe-map (::fs/recipe-id %))
                                   (::fs/amount %)
                                   (::fs/amount-unit %))
                 (::fs/mixins flavor)))))

(defrecord FlavorManager [database recipe-manager]
  component/Lifecycle

  (start [this]
    (assoc this ::flavors {}))

  (stop [this]
    (assoc this ::flavors nil)))

(defn make-flavor-manager []
  (map->FlavorManager {}))
