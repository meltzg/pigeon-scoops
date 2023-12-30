(ns pigeon-scoops.components.grocery-manager
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.basic-spec :as bs]
            [pigeon-scoops.components.config-manager :as cm]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(s/def ::type #(= (namespace ::g) (namespace %)))
(s/def ::description ::bs/non-empty-string)

(s/def ::source ::bs/non-empty-string)
(s/def ::unit-volume pos?)
(s/def ::unit-volume-type (set (keys vol/conversion-map)))
(s/def ::unit-mass pos?)
(s/def ::unit-mass-type (set (keys mass/conversion-map)))
(s/def ::unit-common pos?)
(s/def ::unit-common-type units/other-units)
(s/def ::unit-cost pos?)
(s/def ::unit-purchase-quantity pos-int?)

(s/def ::unit (s/keys :req [::source
                            ::unit-cost]
                      :opt [::unit-mass
                            ::unit-mass-type
                            ::unit-volume
                            ::unit-volume-type
                            ::unit-common
                            ::unit-common-type
                            ::unit-purchase-quantity]))
(s/def ::units (s/coll-of ::unit))

(s/def ::entry (s/keys :req [::type
                             ::units]
                       :opt [::description
                             ::amount-needed
                             ::amount-needed-unit]))

(defrecord GroceryManager [config-manager]
  component/Lifecycle

  (start [this]
    (assoc this ::groceries (-> config-manager
                                ::cm/app-settings
                                ::cm/groceries-file
                                slurp
                                edn/read-string
                                atom)))

  (stop [this]
    (logger/info "Saving changes to groceries")
    (spit (-> config-manager ::cm/app-settings ::cm/groceries-file)
          (with-out-str (clojure.pprint/pprint (deref (::groceries this)))))
    (assoc this ::groceries nil)))

(defn make-grocery-manager []
  (map->GroceryManager {}))

(defn get-groceries [grocery-manager & types]
  (cond->> (deref (::groceries grocery-manager))
           (not-empty types) (filter #(some #{(::type %)} types))))

(defn add-grocery-item
  ([grocery-manager new-grocery-item]
   (add-grocery-item grocery-manager new-grocery-item false))
  ([grocery-manager new-grocery-item update?]
   (let [existing (first (get-groceries grocery-manager (::type new-grocery-item)))]
     (if-not (or (and update? (not existing))
                 (and (not update?) existing))
       (or (s/explain-data ::entry new-grocery-item)
           (swap! (::groceries grocery-manager)
                  (fn [groceries]
                    (conj (remove #(= (::type %) (::type new-grocery-item)) groceries) new-grocery-item))))))))

(defn delete-grocery-item [grocery-manager type]
  (logger/info (str "Deleting " type))
  (swap! (::groceries grocery-manager)
         (partial remove #(= (::type %) type))))

(defn get-grocery-unit-for-amount [amount amount-unit {::keys [units]}]
  (let [unit-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit)))
        unit-type-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit) "-type"))
        unit-comparator #(units/to-comparable (unit-key %) (unit-type-key %))]
    (try
      (or (first (filter #(>= (units/convert (unit-key %) (unit-type-key %) amount-unit) amount)
                         (sort-by unit-comparator units)))
          (first (sort-by (comp - unit-comparator) units)))
      (catch NullPointerException e
        nil))))

(defn divide-grocery [amount amount-unit grocery-item]
  (assoc (->> (loop [grocery-units {}
                     amount-left amount]
                (let [grocery-unit (get-grocery-unit-for-amount amount-left amount-unit grocery-item)
                      unit-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit)))
                      unit-type-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit) "-type"))]
                  (if (or (nil? grocery-unit) (<= amount-left 0))
                    grocery-units
                    (recur (update grocery-units grocery-unit (fnil inc 0))
                           (- amount-left (units/convert (unit-key grocery-unit)
                                                         (unit-type-key grocery-unit)
                                                         amount-unit))))))
              (reduce-kv #(assoc %1 (assoc %2 ::unit-purchase-quantity %3) %3) {})
              keys
              (assoc grocery-item ::units))
    ::amount-needed amount
    ::amount-needed-unit amount-unit))
