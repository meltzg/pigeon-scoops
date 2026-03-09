(ns pigeon-scoops.components.select-options-sizer
  (:require
   [uix.core :as uix :refer [$ defui]]))

(defui select-options-sizer [{:keys [options on-size-change]}]
  (let [measure-ref (uix/use-ref nil)]
    (uix/use-effect
     (fn []
       (when @measure-ref
         (let [widths (map #(.-offsetWidth %) (.-children @measure-ref))]
           (on-size-change (str (+ (apply max widths) 45) "px")))))
     [on-size-change options])
    ($ :div {:style {:position "absolute"
                     :visibility "hidden"
                     :height 0}
             :ref measure-ref}
       (when (seq options)
         (for [{:keys [label value]} options]
           ($ :div {:key value
                    :style {:fontSize "14px"}}
              label))))))