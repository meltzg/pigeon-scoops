(ns pigeon-scoops.controls.unit-selector
  (:require [uix.core :as uix :refer [$ defui]]
            [antd :refer [Select Spin]]
            [pigeon-scoops.hooks :refer [use-constants]]))

(defui unit-selector [{:keys [value on-change valid-type-categories]}]
  (let [{:keys [constants loading?]} (use-constants)
        [unit-types set-unit-types!] (uix/use-state [])]
    (uix/use-effect
     (fn []
       (prn "Unit types updated:" constants loading?)
       (when-not loading?
         (let [grouped-types (update-keys (->> constants
                                               :constants/unit-types
                                               (group-by namespace))
                                          keyword)
               valid-type-categories (or valid-type-categories (keys grouped-types))] 
           (set-unit-types! (->> valid-type-categories
                                 (select-keys grouped-types)
                                 (vals)
                                 (apply concat))))))
     [constants loading? valid-type-categories])
    (if (or loading? (not (seq unit-types)))
      ($ Spin)
      ($ Select {:value     value
                 :on-change #(on-change (keyword %))
                 :options (clj->js (for [ut unit-types]
                                     {:value (.substring (str ut) 1)
                                      :label (name ut)}))}))))