(ns pigeon-scoops.components.flavor-manager
  (:require [clojure.tools.logging :as logger]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [select
                                       from
                                       where
                                       delete-from
                                       insert-into
                                       values]]
            [next.jdbc :as jdbc]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.spec.flavors :as fs])
  (:import (java.util UUID)))

(def create-flavor-table-statement {:create-table [:flavors :if-not-exists]
                                    :with-columns
                                    [[:id :uuid [:not nil] :primary-key]
                                     [:name :text [:not nil]]
                                     [:instructions :text :array]
                                     [:recipe-id :uuid [:references :recipes :id] [:not nil]]
                                     [:amount :real [:not nil]]
                                     [:amount-unit :text [:not nil]]
                                     [:amount-unit-type :text [:not nil]]]})

(def create-mixin-table {:create-table [:mixins :if-not-exists]
                         :with-columns
                         [[:flavor-id :uuid [:references :flavors :id] [:not nil]]
                          [:recipe-id :uuid [:references :recipes :id] [:not nil]]
                          [:amount :real [:not nil]]
                          [:amount-unit :text [:not nil]]
                          [:amount-unit-type :text [:not nil]]
                          [:primary :key [:composite :flavor-id :recipe-id]]]})

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
   (logger/info (str "Adding " ::fs/name))
   (let [flavor-id (if update?
                     (::fs/id new-flavor)
                     (or (::fs/id new-flavor) (UUID/randomUUID)))
         existing (first (get-flavors! flavor-manager flavor-id))
         new-flavor (assoc new-flavor ::fs/id flavor-id)
         flavor-statement (-> (insert-into :flavors)
                              (values [(-> new-flavor
                                           (dissoc ::fs/mixins)
                                           (update ::fs/amount-unit name)
                                           (update ::fs/instructions #(vec [:array % :text]))
                                           (assoc ::fs/amount-unit-type (namespace (::fs/amount-unit new-flavor)))
                                           (update-keys (comp keyword name)))])
                              sql/format)
         mixin-statement (-> (insert-into :mixins)
                             (values (map #(conj {:flavor-id        flavor-id
                                                  :amount-unit-type (namespace (::fs/amount-unit %))}
                                                 (update-keys
                                                   (update % ::fs/amount-unit name)
                                                   (comp keyword name)))
                                          (::fs/mixins new-flavor)))
                             sql/format)]
     (or (s/explain-data ::fs/entry new-flavor)
         (when-not (or (and update? (not existing))
                       (and (not update?) existing))
           (jdbc/with-transaction
             [conn (-> flavor-manager :database ::db/connection)]
             (unsafe-delete-flavor! conn flavor-id)
             (jdbc/execute! conn
                            flavor-statement)
             (when-not (empty? (::fs/mixins new-flavor))
               (jdbc/execute! conn
                              mixin-statement))
             new-flavor))))))

(defrecord FlavorManager [database recipe-manager]
  component/Lifecycle

  (start [this]
    (jdbc/execute! (::db/connection database) (sql/format create-flavor-table-statement))
    (jdbc/execute! (::db/connection database) (sql/format create-mixin-table))
    (assoc this ::flavors {}))

  (stop [this]
    (assoc this ::flavors nil)))

(defn make-flavor-manager []
  (map->FlavorManager {}))
