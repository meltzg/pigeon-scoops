(ns pigeon-scoops.context
  (:require [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.hooks :refer [use-token]]))

(def grocery-context (uix/create-context))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (-> (js/fetch "https://api.pigeon-scoops.com/v1/groceries"
                           (clj->js {:method  "GET"
                                     :headers {:Accept        "application/transit+json"
                                               :Authorization (str "Bearer " token)}}))
                 (.then (fn [resp]
                          (prn resp)))))
           [token refresh?])
         ($ (.-Provider grocery-context) {:value {:groceries groceries
                                                  :set-refresh! set-refresh!}}
            children)))
