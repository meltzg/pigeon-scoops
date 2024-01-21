(ns pigeon-scoops.core
  (:require [ajax.core :as ajax]
            [pigeon-scoops.apps.groceries :refer [grocery-list]]
            [pigeon-scoops.apps.recipes :refer [recipe-list]]
            [pigeon-scoops.apps.auth :refer [authenticator]]
            [pigeon-scoops.utils :refer [api-url]]
            [uix.core :as uix :refer [$ defui]]
            [uix.dom]
            ["@mui/icons-material/Menu$default" :as MenuIcon]
            ["@mui/icons-material/LocalGroceryStore$default" :as LocalGroceryStoreIcon]
            ["@mui/icons-material/MenuBook$default" :as MenuBookIcon]
            ["@mui/material" :refer [AppBar
                                     Box
                                     Drawer
                                     IconButton
                                     List
                                     ListItem
                                     ListItemButton
                                     ListItemIcon
                                     ListItemText
                                     Toolbar
                                     Typography]]))

(defui app-menu-item [{:keys [text icon app-key set-active-app!]}]
       ($ ListItem
          ($ ListItemButton {:onClick #(set-active-app! app-key)}
             ($ ListItemIcon
                ($ icon))
             ($ ListItemText {:primary text}))))

(defui content []
       (let [[menu-open? set-menu-open!] (uix/use-state false)
             [active-app set-active-app!] (uix/use-state :recipes)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh-groceries? set-refresh-groceries!] (uix/use-state true)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh-recipes? set-refresh-recipes!] (uix/use-state true)
             [signed-in? set-signed-in!] (uix/use-state false)]
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "signIn")
                       {:handler (partial set-signed-in! true)})))
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "groceries")
                       {:response-format :transit
                        :handler         set-groceries!
                        :error-handler   (fn [_]
                                           (set-groceries! []))}))
           [refresh-groceries?])
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "recipes")
                       {:response-format :transit
                        :handler         set-recipes!
                        :error-handler   (fn [_]
                                           (set-recipes! []))}))
           [refresh-recipes?])
         ($ Box
            ($ AppBar
               ($ Toolbar
                  ($ IconButton {:on-click #(set-menu-open! (not menu-open?))}
                     ($ MenuIcon))
                  ($ Typography {:variant "h6"}
                     "Pigeon Scoops Manager")
                  ($ authenticator {:signed-in? signed-in? :on-change #(do (set-signed-in! %)
                                                                           (set-refresh-groceries! (not refresh-groceries?))
                                                                           (set-refresh-recipes! (not refresh-recipes?)))})))
            ($ Drawer {:anchor   "left"
                       :open     menu-open?
                       :on-close #(set-menu-open! (not menu-open?))}
               ($ List
                  (for [[app-name app-icon app-key] [["Groceries" LocalGroceryStoreIcon :groceries]
                                                     ["Recipes" MenuBookIcon :recipes]]]
                    ($ app-menu-item {:key             app-key
                                      :app-key         app-key
                                      :text            app-name
                                      :icon            app-icon
                                      :set-active-app! #(do (set-active-app! %)
                                                            (set-menu-open! (not menu-open?)))}))))
            ($ Box {:component "div"}
               ($ Toolbar)
               ($ grocery-list {:groceries groceries
                                :on-change #(set-refresh-groceries! (not refresh-groceries?))
                                :active?   (= active-app :groceries)})
               ($ recipe-list {:recipes   recipes
                               :groceries groceries
                               :on-change #(set-refresh-recipes! (not refresh-recipes?))
                               :active?   (= active-app :recipes)})))))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ content) root))

(defn ^:export init []
  (render))
