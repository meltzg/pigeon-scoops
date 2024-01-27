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
            [pigeon-scoops.components.flavor-manager :as fm]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.spec.orders :as os]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs])
  (:import (java.util UUID)))

(def create-order-table-statement {:create-table [:orders :if-not-exists]
                                   :with-columns
                                   [[:id :uuid [:not nil] :primary-key]
                                    [:note :text [:not nil]]]})

(def create-flavor-amounts-table {:create-table [:flavor-amounts :if-not-exists]
                                  :with-columns
                                  [[:order-id :uuid [:references :orders :id] [:not nil]]
                                   [:flavor-id :uuid [:references :flavors :id] [:not nil]]
                                   [:amount :real [:not nil]]
                                   [:amount-unit :text [:not nil]]
                                   [:amount-unit-type :text [:not nil]]
                                   [:primary :key [:composite :order-id :flavor-id]]]})

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

     (println flavor-amounts-statement)
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

(defn to-grocery-list! [order-manager order]
  (let [flavor-map (into {} (map #(vec [(::fs/id %) %])
                                 (apply (partial fm/get-flavors! (:flavor-manager order-manager))
                                        (map ::os/flavor-id (::os/flavors order)))))
        to-grocery-list (fn [recipe-ingredients]
                          (rm/to-grocery-purchase-list recipe-ingredients
                                                       (apply (partial gm/get-groceries! (:grocery-manager order-manager))
                                                              (map ::rs/ingredient-type recipe-ingredients))))]
    (->> order
         ::os/flavors
         (map #(fm/scale-flavor (get flavor-map (::os/flavor-id %))
                                (::os/amount %)
                                (::os/amount-unit %)))
         (map (partial fm/materialize-recipes! (:flavor-manager order-manager)))
         (apply concat)
         rm/merge-recipe-ingredients
         to-grocery-list)))

(defrecord OrderManager [database grocery-manager recipe-manager flavor-manager]
  component/Lifecycle

  (start [this]
    (jdbc/execute! (::db/connection database) (sql/format create-order-table-statement))
    (jdbc/execute! (::db/connection database) (sql/format create-flavor-amounts-table))
    (assoc this ::orders {}))

  (stop [this]
    (assoc this ::orders nil)))

(defn make-flavor-manager []
  (map->OrderManager {}))
