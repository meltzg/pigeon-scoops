(ns pigeon-scoops.controls.constants-selector
  (:require
   [antd :refer [Form Select Spin]]
   [pigeon-scoops.components.select-options-sizer :refer [select-options-sizer]]
   [pigeon-scoops.hooks :refer [use-constants]]
   [pigeon-scoops.utils :refer [parse-keyword stringify-keyword]]
   [uix.core :as uix :refer [$ defui]]))

(defui constants-selector [{:keys [form-item-name constants-key valid-namespaces required? value on-change disabled?]}]
  (let [{:keys [constants loading?]} (use-constants)
        [options set-options!] (uix/use-state [])
        [select-width set-select-width!] (uix/use-state "auto")]
    (uix/use-effect
     (fn []
       (when-not loading?
         (let [grouped-types (update-keys (->> constants
                                               constants-key
                                               (group-by namespace))
                                          keyword)
               valid-type-categories (or valid-namespaces (keys grouped-types))]
           (set-options! (for [const (->> valid-type-categories
                                          (select-keys grouped-types)
                                          (vals)
                                          (apply concat))]
                           {:value (stringify-keyword const)
                            :label (name const)})))))
     [constants constants-key loading? valid-namespaces])

    (if (or loading? (not (seq options)))
      ($ Spin)
      ($ :div
         ($ select-options-sizer {:options options
                                  :on-size-change set-select-width!})
         ($ Form.Item (cond-> {:get-value-from-event parse-keyword
                               :get-value-props (fn [value]
                                                  (clj->js {:value (stringify-keyword value)}))
                               :rules (clj->js [{:required required?}])}
                        (not (nil? form-item-name)) (assoc :name form-item-name))
            ($ Select (cond-> {:on-change on-change
                               :style (clj->js {:width select-width})
                               :options (clj->js options)
                               :disabled disabled?} 
                        (not (nil? value)) (assoc :value (stringify-keyword value)))))))))