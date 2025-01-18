(ns pigeon-scoops.core
  (:require [uix.core :as uix :refer [$ defui]]
            [uix.dom]
            [pigeon-scoops.auth :refer [authenticator]]
            ["@auth0/auth0-react" :refer [Auth0Provider]]
            ["@mui/icons-material/Menu$default" :as MenuIcon]
            ["@mui/icons-material/Icecream$default" :as IcecreamIcon]
            ["@mui/icons-material/LocalGroceryStore$default" :as LocalGroceryStoreIcon]
            ["@mui/icons-material/MenuBook$default" :as MenuBookIcon]
            ["@mui/icons-material/Receipt$default" :as ReceiptIcon]
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

(defui app-menu-item [{:keys [text icon]}]
       ($ ListItem
          ($ ListItemButton
             ($ ListItemIcon
                ($ icon))
             ($ ListItemText {:primary text}))))

(defui content []
       (let [[menu-open? set-menu-open!] (uix/use-state false)]
         ($ Box
            ($ AppBar
               ($ Toolbar
                  ($ IconButton {:on-click #(set-menu-open! (not menu-open?))}
                     ($ MenuIcon))
                  ($ Typography {:variant "h6"}
                     "Pigeon Scoops Manager")
                  ($ Box {:ml "auto"}
                     ($ authenticator))))
            ($ Drawer {:anchor   "left"
                       :open     menu-open?
                       :on-close #(set-menu-open! (not menu-open?))}
               ($ List
                  (for [[app-name app-icon app-key] [["Groceries" LocalGroceryStoreIcon :groceries]
                                                     ["Recipes" MenuBookIcon :recipes]
                                                     ["Flavors" IcecreamIcon :flavors]
                                                     ["Orders" ReceiptIcon :orders]]]
                    ($ app-menu-item {:key  app-key
                                      :text app-name
                                      :icon app-icon})))))))

(defui app []
       ($ Auth0Provider {:domain               "pigeon-scoops.us.auth0.com"
                         :client-id            "AoU9LnGWQlCbSUvjgXdHf4NZPJh0VHYD"
                         :authorization-params (clj->js {:redirect_uri (.. js/window -location -origin)})}
          ($ content)))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
