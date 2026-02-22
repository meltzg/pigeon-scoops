(ns pigeon-scoops.controls.constants-selector
  (:require [uix.core :as uix :refer [$ defui]]
            [antd :refer [Select Spin]]
            [pigeon-scoops.hooks :refer [use-constants]]))

(defui constants-selector [{:keys [value on-change constants-key valid-namespaces]}]
  (let [{:keys [constants loading?]} (use-constants)
        [options set-options!] (uix/use-state [])]
    (uix/use-effect
     (fn []
       (when-not loading?
         (let [grouped-types (update-keys (->> constants
                                               constants-key
                                               (group-by namespace))
                                          keyword)
               valid-type-categories (or valid-namespaces (keys grouped-types))]
           (set-options! (->> valid-type-categories
                              (select-keys grouped-types)
                              (vals)
                              (apply concat))))))
     [constants constants-key loading? valid-namespaces])
    (if (or loading? (not (seq options)))
      ($ Spin)
      ($ Select {:value     value
                 :on-change #(on-change (keyword %))
                 :options (clj->js (for [ut options]
                                     {:value (.substring (str ut) 1)
                                      :label (name ut)}))}))))