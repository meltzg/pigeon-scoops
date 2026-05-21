(ns pigeon-scoops.production.views
  (:require [antd :refer [Checkbox Flex Input Spin]]
            [cljs.pprint :refer [pprint]]
            [pigeon-scoops.hooks :refer [use-production-items]]
            [uix.core :as uix :refer [$ defui]]))

(defui item-production-view []
  (let [{:keys [production-items loading?]} (use-production-items true)]
    (if loading?
      ($ Spin)
      ($ Input.TextArea
         {:style {:height "100%"}
          :value (with-out-str
                   (pprint production-items))}))))

(defui recipe-production-view []
  (let [{:keys [production-items loading?]} (use-production-items false)]
    (if loading?
      ($ Spin)
      ($ Input.TextArea
         {:style {:height "100%"}
          :value (with-out-str
                   (pprint production-items))}))))

(defui production-view []
  (let [[separate-sizes? set-separate-sizes!] (uix/use-state false)]
    ($ Flex {:gap "small" :vertical true :style {:height "100%"}}
       ($ Checkbox {:value separate-sizes?
                    :on-change #(set-separate-sizes! (.. % -target -checked))}
          "Separate Sizes")
       (if separate-sizes?
         ($ item-production-view)
         ($ recipe-production-view)))))
