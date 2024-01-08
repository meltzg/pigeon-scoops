(ns pigeon-scoops.components.grocery-manager
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.components.config-manager :as cm]
            [pigeon-scoops.components.db :as db]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.spec.groceries :as gs]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where]]
            [next.jdbc :as jdbc]))

(def create-grocery-table-statement {:create-table [:groceries :if-not-exists]
                                     :with-columns
                                     [[:type :text [:not nil] :primary-key]
                                      [:description :text]]})

(def create-grocery-unit-table-statement {:create-table [:grocery-units :if-not-exists]
                                          :with-columns
                                          [[:type :text [:references :groceries :type] [:not nil]]
                                           [:source :text [:not nil]]
                                           [:unit-cost :real [:not nil]]
                                           [:unit-mass :real]
                                           [:unit-mass-type :text]
                                           [:unit-volume :real]
                                           [:unit-volume-type :text]
                                           [:unit-common :real]
                                           [:unit-common-type :text]]})

(defrecord GroceryManager [config-manager database]
  component/Lifecycle

  (start [this]
    (jdbc/execute! (::db/connection database) (sql/format create-grocery-table-statement))
    (jdbc/execute! (::db/connection database) (sql/format create-grocery-unit-table-statement))
    (assoc this ::groceries {}))

  (stop [this]
    (logger/info "Saving changes to groceries")
    (spit (-> config-manager ::cm/app-settings ::cm/groceries-file)
          (with-out-str (pprint (deref (::groceries this)))))
    (assoc this ::groceries nil)))

(defn make-grocery-manager []
  (map->GroceryManager {}))

(defn from-db-namespace [entity]
  (->> (update-keys entity #(keyword (namespace ::gs/entry) (name %)))
       (filter #(some? (second %)))
       (into {})))

(defn from-db [grocery-items units]
  (println grocery-items)
  (println units)
  (->> grocery-items
       (map (fn [item]
              (assoc item ::gs/units (map from-db-namespace
                                          (filter #(= (:grocery_units/type %)
                                                      (:groceries/type item)) units)))))
       (map from-db-namespace)))

(defn get-groceries [grocery-manager & types]
  (cond->> (deref (::groceries grocery-manager))
           (not-empty types) (filter #(some #{(::gs/type %)} types))))

(defn get-groceries! [grocery-manager & types]
  (let [items (jdbc/execute! (-> grocery-manager :database ::db/connection)
                             (cond-> (select :*)
                                     true (from :groceries)
                                     (not-empty types) (where [:in :type (map name types)])
                                     true sql/format))
        units (jdbc/execute! (-> grocery-manager :database ::db/connection)
                             (-> (select :*)
                                 (from :grocery-units)
                                 (where [:in :type (map :groceries/type items)])
                                 sql/format))]
    (from-db items units)))

(defn add-grocery-item
  ([grocery-manager new-grocery-item]
   (add-grocery-item grocery-manager new-grocery-item false))
  ([grocery-manager new-grocery-item update?]
   (let [existing (first (get-groceries grocery-manager (::gs/type new-grocery-item)))]
     (when-not (or (and update? (not existing))
                   (and (not update?) existing))
       (or (s/explain-data ::gs/entry new-grocery-item)
           (swap! (::groceries grocery-manager)
                  (fn [groceries]
                    (conj (remove #(= (::gs/type %) (::gs/type new-grocery-item)) groceries) new-grocery-item))))))))

(defn add-grocery-item!
  ([grocery-manager new-grocery-item]
   (add-grocery-item! grocery-manager new-grocery-item false))
  ([grocery-manager new-grocery-manager update?]))

(defn delete-grocery-item [grocery-manager type]
  (logger/info (str "Deleting " type))
  (swap! (::groceries grocery-manager)
         (partial remove #(= (::gs/type %) type))))

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
  (assoc (->> (loop [grocery-units {}
                     amount-left amount]
                (let [grocery-unit (get-grocery-unit-for-amount amount-left amount-unit grocery-item)
                      unit-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit)))
                      unit-type-key (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class amount-unit) "-type"))]
                  (if (or (nil? grocery-unit) (<= amount-left 0))
                    grocery-units
                    (recur (update grocery-units grocery-unit (fnil inc 0))
                           (- amount-left (units/convert (unit-key grocery-unit)
                                                         (unit-type-key grocery-unit)
                                                         amount-unit))))))
              (reduce-kv #(assoc %1 (assoc %2 ::gs/unit-purchase-quantity %3) %3) {})
              keys
              (assoc grocery-item ::gs/units))
    ::gs/amount-needed amount
    ::gs/amount-needed-unit amount-unit))
