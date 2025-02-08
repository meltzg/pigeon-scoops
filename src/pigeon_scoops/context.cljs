(ns pigeon-scoops.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def constants-context (uix/create-context))
(def groceries-context (uix/create-context))
(def recipes-context (uix/create-context))

(defui with-constants [{:keys [children]}]
       (let [[constants set-constants!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (.then (api/get-constants) set-constants!))
           [])
         ($ (.-Provider constants-context) {:value constants}
            children)))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (.then (api/get-groceries token) set-groceries!)))
           [token refresh?])
         ($ (.-Provider groceries-context) {:value {:groceries groceries
                                                    :refresh!  #(set-refresh! (not refresh?))}}
            children)))

(defui with-recipes [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (.then (api/get-recipes token) set-recipes!)))
           [token refresh?])
         ($ (.-Provider recipes-context) {:value {:recipes  (apply concat (vals recipes))
                                                  :refresh! #(set-refresh! (not refresh?))}}
            children)))

