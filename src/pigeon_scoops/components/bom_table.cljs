(ns pigeon-scoops.components.bom-table
  (:require
   [antd :refer [Descriptions Space Table]]
   [clojure.string :as str]
   [goog.string :as gstring]
   [goog.string.format]
   [pigeon-scoops.utils :refer [make-sorter stringify-keyword]]
   [uix.core :refer [$ defui]]))

(defn format-amount [amount amount-unit]
  (when amount
    (str amount (name amount-unit))))

(defn format-dollar [amount]
  (str "$" (gstring/format "%.2f" amount)))

(defn format-percentage [decimal]
  (str (gstring/format "%.2f" (* decimal 100)) "%"))

(defn purchase-cost [units]
  (->> units
       (map #(* (:grocery-unit/unit-cost %)
                (:grocery-unit/quantity %)))
       (reduce +)))

(defn required-cost [units waste-ratio]
  (let [purchase-cost (purchase-cost units)]
    (- purchase-cost (* purchase-cost waste-ratio))))

(defn format-unit [{:grocery-unit/keys [quantity
                                        unit-mass
                                        unit-mass-type
                                        unit-volume
                                        unit-volume-type
                                        unit-common
                                        unit-common-type]}]
  (let [unit (->> [unit-mass unit-mass-type
                   unit-volume unit-volume-type
                   unit-common unit-common-type]
                  (partition 2)
                  (filter (comp some? first))
                  (map #(apply format-amount %))
                  (str/join ":"))]
    (str quantity " x " unit)))

(defn add-derived-fields [grocery]
  (let [{:grocery/keys [required-amount
                        required-unit
                        purchase-amount
                        purchase-unit
                        waste-ratio
                        units]} grocery
        purchase-cost (purchase-cost units)
        required-cost (required-cost units waste-ratio)
        formatted-units (str/join " & " (map format-unit units))]
    (assoc grocery
           :grocery/amount-needed (format-amount required-amount required-unit)
           :grocery/amount-cost required-cost
           :grocery/purchase-amount (format-amount purchase-amount purchase-unit)
           :grocery/purchase-units formatted-units
           :grocery/purchase-cost purchase-cost
           :grocery/waste-ratio waste-ratio)))

(def columns
  [{:title "Item"
    :dataIndex (stringify-keyword :grocery/name)
    :sorter (make-sorter :grocery/name)
    :key :name}
   {:title "Department"
    :dataIndex (stringify-keyword :grocery/department)
    :render #(last (str/split % #"/"))
    :sorter (make-sorter :grocery/department)
    :key :department}
   {:title "Amount Needed"
    :dataIndex (stringify-keyword :grocery/amount-needed)
    :key :amount-needed}
   {:title "Amount Cost"
    :dataIndex (stringify-keyword :grocery/amount-cost)
    :render format-dollar
    :sorter (make-sorter :grocery/amount-cost)
    :key :amount-cost}
   {:title "Purchase Amount"
    :dataIndex (stringify-keyword :grocery/purchase-amount)
    :key :purchase-amount}
   {:title "Purchase Units"
    :dataIndex (stringify-keyword :grocery/purchase-units)
    :key :purchase-units}
   {:title "Purchase Cost"
    :dataIndex (stringify-keyword :grocery/purchase-cost)
    :render format-dollar
    :sorter (make-sorter :grocery/purchase-cost)
    :key :purchase-cost}
   {:title "Waste Ratio"
    :dataIndex (stringify-keyword :grocery/waste-ratio)
    :render format-percentage
    :sorter (make-sorter :grocery/waste-ratio)
    :key :waste-ratio}])

(defui bom-view [{:keys [groceries]}]
  (let [total-cost (reduce + (map (comp purchase-cost :grocery/units) groceries))
        required-cost (reduce + (map #(required-cost (:grocery/units %)
                                                     (:grocery/waste-ratio %))
                                     groceries))]
    ($ Space {:orientation "vertical"}
       ($ Descriptions {:title "Cost Summary" :column 1 :bordered true}
          ($ Descriptions.Item {:label "Total Cost"}
             (format-dollar total-cost))
          ($ Descriptions.Item {:label "Required Cost"}
             (format-dollar required-cost)))
       ($ Table {:columns (clj->js columns)
                 :dataSource (clj->js (map-indexed (fn [idx grocery]
                                                     (assoc (add-derived-fields grocery) :key idx)) groceries)
                                      :keyword-fn stringify-keyword)
                 :pagination false
                 :row-selection (clj->js {:type "checkbox"})
                 :bordered true}))))
