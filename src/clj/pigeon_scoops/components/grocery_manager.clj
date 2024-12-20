(ns pigeon-scoops.components.grocery-manager
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.spec.groceries :as gs]
            [honey.sql :as sql]
            [honey.sql.helpers :as hsql :refer [select
                                                from
                                                where
                                                delete-from
                                                insert-into
                                                values]]
            [next.jdbc :as jdbc]))

(defn unit-from-db [unit]
  (let [initial (dissoc (db/from-db-namespace ::gs/entry unit) ::gs/type)]
    (cond-> initial
            (::gs/unit-mass-type initial) (update ::gs/unit-mass-type (partial keyword (namespace ::mass/g)))
            (::gs/unit-volume-type initial) (update ::gs/unit-volume-type (partial keyword (namespace ::volume/c)))
            (::gs/unit-common-type initial) (update ::gs/unit-common-type (partial keyword (namespace ::units/pinch))))))

(defn from-db [grocery-items units]
  (->> grocery-items
       (map (fn [item]
              (assoc item ::gs/units (map unit-from-db
                                          (filter #(= (:grocery_units/type %)
                                                      (:groceries/type item)) units)))))
       (map (partial db/from-db-namespace ::gs/entry))
       (map (fn [item]
              (update item ::gs/type #(keyword (namespace ::gs/type) %))))))

(defn get-groceries! [grocery-manager & types]
  (let [items (jdbc/execute! (-> grocery-manager :database ::db/connection)
                             (cond-> (-> (select :*)
                                         (from :groceries))
                                     (not-empty types) (where [:in :type (map name types)])
                                     :then sql/format))
        units (when (not-empty items)
                (jdbc/execute! (-> grocery-manager :database ::db/connection)
                               (-> (select :*)
                                   (from :grocery-units)
                                   (where [:in :type (map :groceries/type items)])
                                   sql/format)))]
    (from-db items units)))

(defn- unsafe-delete-grocery-item! [conn type]
  (jdbc/execute! conn
                 (-> (delete-from :grocery-units)
                     (where [:= :type (name type)])
                     sql/format))
  (jdbc/execute! conn
                 (-> (delete-from :groceries)
                     (where [:= :type (name type)])
                     sql/format)))

(defn delete-grocery-item! [grocery-manager type]
  (logger/info (str "Deleting " type))
  (jdbc/with-transaction [conn (-> grocery-manager :database ::db/connection)]
                         (unsafe-delete-grocery-item! conn type)))

(defn add-grocery-item!
  ([grocery-manager new-grocery-item]
   (add-grocery-item! grocery-manager new-grocery-item false))
  ([grocery-manager new-grocery-item update?]
   (logger/info (str "Adding " ::gs/type new-grocery-item))
   (let [existing (first (get-groceries! grocery-manager (::gs/type new-grocery-item)))
         item-values (-> (update new-grocery-item ::gs/type name)
                         (dissoc ::gs/units)
                         (assoc ::gs/description (::gs/description new-grocery-item))
                         (update-keys (comp keyword name)))]
     (or (s/explain-data ::gs/entry new-grocery-item)
         (when-not (or (and update? (not existing))
                       (and (not update?) existing))
           (jdbc/with-transaction
             [conn (-> grocery-manager :database ::db/connection)]
             (jdbc/execute! conn
                            (-> (delete-from :grocery-units)
                                (where [:= :type (-> new-grocery-item ::gs/type name)])
                                sql/format))
             (jdbc/execute! conn
                            (if update?
                              (-> (hsql/update :groceries)
                                  (hsql/set item-values)
                                  (where [:= :type (-> new-grocery-item ::gs/type name)])
                                  sql/format)
                              (-> (insert-into :groceries)
                                  (values [item-values])
                                  sql/format)))
             (when-not (empty? (::gs/units new-grocery-item))
               (jdbc/execute! conn
                              (-> (insert-into :grocery-units)
                                  (values (map #(conj {:type (-> new-grocery-item ::gs/type name)}
                                                      (update-keys
                                                        (cond-> %
                                                                (::gs/unit-mass-type %) (update ::gs/unit-mass-type name)
                                                                (::gs/unit-volume-type %) (update ::gs/unit-volume-type name)
                                                                (::gs/unit-common-type %) (update ::gs/unit-common-type name))
                                                        (comp keyword name)))
                                               (::gs/units new-grocery-item)))
                                  sql/format)))
             new-grocery-item))))))

(defrecord GroceryManager [database]
  component/Lifecycle

  (start [this]
    (->> "groceries.edn"
         io/resource
         slurp
         edn/read-string
         (map (partial add-grocery-item! this))
         doall)
    (assoc this ::groceries {}))

  (stop [this]
    (assoc this ::groceries nil)))

(defn make-grocery-manager []
  (map->GroceryManager {}))

(defn get-grocery-unit-for-amount [amount amount-unit {::gs/keys [units]}]
  (let [unit-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit)))
        unit-type-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit) "-type"))
        unit-comparator #(units/to-comparable (unit-key %) (unit-type-key %))]
    (try
      (or (first (filter #(>= (units/convert (unit-key %) (unit-type-key %) amount-unit) amount)
                         (sort-by unit-comparator units)))
          (first (sort-by (comp - unit-comparator) units)))
      (catch NullPointerException _
        nil))))

(defn divide-grocery [amount amount-unit grocery-item]
  (let [unit-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit)))
        unit-type-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit) "-type"))
        units (->> (loop [grocery-units {}
                          amount-left amount]
                     (let [grocery-unit (get-grocery-unit-for-amount amount-left amount-unit grocery-item)]
                       (if (or (nil? grocery-unit) (<= amount-left 0))
                         grocery-units
                         (recur (update grocery-units grocery-unit (fnil inc 0))
                                (- amount-left (units/convert (unit-key grocery-unit)
                                                              (unit-type-key grocery-unit)
                                                              amount-unit))))))
                   (reduce-kv #(assoc %1 (assoc %2 ::gs/unit-purchase-quantity %3) %3) {})
                   keys
                   (assoc grocery-item ::gs/units))
        [purchase-amount purchase-unit] (when-let [units (::gs/units units)]
                                          (->> units
                                               (map #(select-keys % [unit-key unit-type-key ::gs/unit-purchase-quantity]))
                                               (map #(update % unit-key * (::gs/unit-purchase-quantity %)))
                                               (mapcat #(take 2 (vals %)))
                                               (apply units/add-amounts)))
        purchase-cost (when-let [units (::gs/units units)]
                        (->> units
                             (map #(select-keys % [::gs/unit-purchase-quantity ::gs/unit-cost]))
                             (map #(apply * (vals %)))
                             (apply +)))]
    (assoc units
      ::gs/amount-needed amount
      ::gs/amount-needed-unit amount-unit
      ::gs/amount-needed-cost (when purchase-cost
                                (* purchase-cost
                                   (units/scale-factor purchase-amount purchase-unit
                                                       amount amount-unit)))
      ::gs/purchase-amount purchase-amount
      ::gs/purchase-amount-unit purchase-unit
      ::gs/purchase-cost purchase-cost)))
