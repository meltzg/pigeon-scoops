(ns pigeon-scoops.components.bom-table
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [uix.core :refer [$ defui]]
            ["@mui/material" :refer [Checkbox
                                     Paper
                                     Stack
                                     TableContainer
                                     Table
                                     TableHead
                                     TableBody
                                     TableRow
                                     TableCell
                                     Typography]]))

(defn format-amount [amount amount-unit]
  (when amount
    (str amount (name amount-unit))))

(defn format-dollar [amount]
  (str "$" (gstring/format "%.2f" amount)))

(defn format-percentage [decimal]
  (str (gstring/format "%.2f" (* decimal 100)) "%"))

(defn kebab-to-title [kebab-str]
  (->> (str/split kebab-str #"-")                           ; Split the string by "-"
       (map str/capitalize)                                 ; Capitalize each word
       (str/join " ")))                                     ; Join them with a space

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

(defui grocery-row [{:keys [grocery]}]
       (let [{:grocery/keys [name
                             required-amount
                             required-unit
                             purchase-amount
                             purchase-unit
                             waste-ratio
                             units]} grocery
             purchase-cost (purchase-cost units)
             required-cost (required-cost units waste-ratio)
             formatted-units (str/join " & " (map format-unit units))]
         ($ TableRow
            ($ TableCell
               ($ Stack {:direction "row"}
                  ($ Checkbox)
                  ($ Typography name)))
            ($ TableCell
               ($ Typography (format-amount required-amount required-unit)))
            ($ TableCell
               ($ Typography (format-dollar required-cost)))
            ($ TableCell
               ($ Typography (format-amount purchase-amount purchase-unit)))
            ($ TableCell
               ($ Typography formatted-units))
            ($ TableCell
               ($ Typography (format-dollar purchase-cost)))
            ($ TableCell
               ($ Typography (format-percentage waste-ratio))))))

(defui groceries-table [{:keys [groceries]}]
       ($ TableContainer {:component Paper}
          ($ Table
             ($ TableHead
                ($ TableRow
                   ($ TableCell "Item")
                   ($ TableCell "Amount Needed")
                   ($ TableCell "Amount Cost")
                   ($ TableCell "Purchase Amount")
                   ($ TableCell "Purchase Units")
                   ($ TableCell "Purchase Cost")
                   ($ TableCell "Waste")))
             ($ TableBody
                (for [g (sort-by :grocery/name groceries)]
                  ($ grocery-row {:key     (select-keys g [:grocery/id :grocery/required-amount :grocery/required-unit])
                                  :grocery g}))))))

(defui bom-view [{:keys [groceries]}]
       (let [total-cost (reduce + (map (comp purchase-cost :grocery/units) groceries))
             required-cost (reduce + (map #(required-cost (:grocery/units %)
                                                          (:grocery/waste-ratio %))
                                          groceries))
             groceries (group-by :grocery/department groceries)]
         ($ Paper
            ($ Typography {:variant "h6"}
               (str "Purchase Cost: " (format-dollar total-cost)))
            ($ Typography {:variant "h6"}
               (str "Required Cost: " (format-dollar required-cost)))
            (for [[department groceries] groceries]
              ($ Stack {:key department :direction "column"}
                 ($ Typography {:variant "h6"}
                    (str "Department: " (kebab-to-title (name department))))
                 ($ groceries-table {:groceries groceries}))))))


