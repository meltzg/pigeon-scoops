(ns pigeon-scoops.menu.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def menus-context (uix/create-context))

(defui with-menus [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [menus set-menus!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             refresh! #(set-refresh! (not refresh?))
             delete! (fn [menu-id]
                       (-> (api/delete-menu token menu-id)
                           (.then refresh!)))]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-menus token)
                   (.then set-menus!))))
           [token refresh?])
         ($ (.-Provider menus-context) {:value {:menus menus
                                                :new-menu! #(do
                                                              (set-menus! (conj menus {:menu/id :new}))
                                                              :new)
                                                :refresh! refresh!
                                                :delete! delete!}})))
