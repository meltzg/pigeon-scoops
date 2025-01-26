(ns pigeon-scoops.context
  (:require [cognitect.transit :as transit]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def grocery-context (uix/create-context))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             reader (transit/reader :json)]
         (uix/use-effect
           (fn []
             (api/get-groceries token set-groceries!))
           [token refresh? reader])
         ($ (.-Provider grocery-context) {:value {:groceries groceries
                                                  :refresh!  #(set-refresh! (not refresh?))}}
            children)))
