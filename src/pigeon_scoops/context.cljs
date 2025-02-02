(ns pigeon-scoops.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def constants-context (uix/create-context))
(def groceries-context (uix/create-context))

(defui with-constants [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [constants set-constants!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (.then (api/get-constants token) set-constants!))
           [token])
         ($ (.-Provider constants-context) {:value constants}
            children)))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (.then (api/get-groceries token) set-groceries!))
           [token refresh?])
         ($ (.-Provider groceries-context) {:value {:groceries groceries
                                                    :refresh!  #(set-refresh! (not refresh?))}}
            children)))
