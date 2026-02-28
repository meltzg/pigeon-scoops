(ns pigeon-scoops.controls.constants-selector
  (:require
   [antd :refer [Form Select Spin]]
   [pigeon-scoops.hooks :refer [use-constants]]
   [pigeon-scoops.utils :refer [parse-keyword stringify-keyword]]
   [uix.core :as uix :refer [$ defui]]))

(defui constants-selector [{:keys [form-item-name constants-key valid-namespaces required? on-change]}]
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
      ($ Form.Item (cond-> {:get-value-from-event parse-keyword
                             :get-value-props (fn [value]
                                                (clj->js {:value (stringify-keyword value)}))
                             :rules (clj->js [{:required required?}])}
                         (not (nil? form-item-name)) (assoc :name form-item-name))
         ($ Select {:on-change on-change
                    :options (clj->js (for [ut options]
                                        {:value (stringify-keyword ut)
                                         :label (name ut)}))})))))