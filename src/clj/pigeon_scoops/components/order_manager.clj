(ns pigeon-scoops.components.order-manager
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
            [pigeon-scoops.spec.orders :as os])
  (:import (java.util UUID)))

(defn order-amount-from-db [order-amount]
  (-> (db/from-db-namespace ::os/entry order-amount)
      (dissoc ::os/order-id ::os/amount-unit-type)
      (update ::os/amount-unit #(keyword (:flavor_amounts/amount_unit_type order-amount) %))))

(defn from-db [orders order-amounts]
  (->> orders
       (map (partial db/from-db-namespace ::os/entry))
       (map (fn [order]
              (assoc order ::os/flavors (map order-amount-from-db
                                             (filter #(= (:flavor_amounts/order_id %)
                                                         (::os/id order)) order-amounts)))))))

(defn get-orders! [order-manager & ids]
  (let [orders (jdbc/execute! (-> order-manager :database ::db/connection)
                              (cond-> (-> (select :*)
                                          (from :orders))
                                      (not-empty ids) (where [:in :id ids])
                                      :then sql/format))
        flavor-amounts (when (not-empty orders)
                         (jdbc/execute! (-> order-manager :database ::db/connection)
                                        (-> (select :*)
                                            (from :flavor-amounts)
                                            (where [:in :order-id (map :orders/id orders)])
                                            sql/format)))]
    (from-db orders flavor-amounts)))

(defn unsafe-delete-order! [conn id]
  (jdbc/execute! conn
                 (-> (delete-from :flavor-amounts)
                     (where [:= :order-id id])
                     sql/format))
  (jdbc/execute! conn
                 (-> (delete-from :orders)
                     (where [:= :id id])
                     sql/format)))

(defn delete-order! [order-manager id]
  (logger/info (str "Deleting " id))
  (jdbc/with-transaction [conn (-> order-manager :database ::db/connection)]
                         (unsafe-delete-order! conn id)))

(defn add-order!
  ([order-manager new-order]
   (add-order! order-manager new-order false))
  ([order-manager new-order update?]
   (logger/info (str "Adding " ::os/id))
   (let [order-id (if update?
                    (::os/id new-order)
                    (or (::os/id new-order) (UUID/randomUUID)))
         existing (first (get-orders! order-manager order-id))
         new-order (assoc new-order ::os/id order-id)
         order-values (-> new-order
                          (dissoc ::os/flavors)
                          (update-keys (comp keyword name)))
         order-statement (if update?
                           (-> (hsql/update :orders)
                               (hsql/set order-values)
                               (where [:= :id order-id])
                               sql/format)
                           (-> (insert-into :orders)
                               (values [order-values])
                               sql/format))
         flavor-amounts-statement (-> (insert-into :flavor-amounts)
                                      (values (map #(conj {:order-id         order-id
                                                           :amount-unit-type (namespace (::os/amount-unit %))}
                                                          (update-keys
                                                            (update % ::os/amount-unit name)
                                                            (comp keyword name)))
                                                   (::os/flavors new-order)))
                                      sql/format)]

     (or (s/explain-data ::os/entry new-order)
         (when-not (or (and update? (not existing))
                       (and (not update?) existing))
           (jdbc/with-transaction
             [conn (-> order-manager :database ::db/connection)]
             (jdbc/execute! conn
                            (-> (delete-from :flavor-amounts)
                                (where [:= :order-id order-id])
                                sql/format))
             (jdbc/execute! conn
                            order-statement)
             (when-not (empty? (::os/flavors new-order))
               (jdbc/execute! conn
                              flavor-amounts-statement))
             new-order))))))

(defrecord OrderManager [database grocery-manager recipe-manager flavor-manager]
  component/Lifecycle

  (start [this]
    (assoc this ::orders {}))

  (stop [this]
    (assoc this ::orders nil)))

(defn make-flavor-manager []
  (map->OrderManager {}))
