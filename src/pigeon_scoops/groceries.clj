(ns pigeon-scoops.groceries
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(s/def ::type #(= (namespace ::g) (namespace %)))
(s/def ::description :basic-spec/non-empty-string)

(s/def ::source :basic-spec/non-empty-string)
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
                       :opt [::description]))

(defn add-grocery-item [groceries new-grocery-item]
  (let [conformed-ingredient (s/conform ::entry new-grocery-item)]
    (if (s/invalid? conformed-ingredient)
      groceries
      (conj (remove #(= (::type %) (::type new-grocery-item)) groceries) conformed-ingredient))))

(defn get-grocery-unit-for-amount [amount amount-unit {::keys [units]}]
  (let [unit-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit)))
        unit-type-key (keyword (namespace ::g) (str "unit-" (units/to-unit-class amount-unit) "-type"))
        unit-comparator #(units/to-comparable (unit-key %) (unit-type-key %))]
    (println unit-key unit-type-key unit-comparator)
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
