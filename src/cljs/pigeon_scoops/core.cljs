(ns pigeon-scoops.core
  (:require [ajax.core :as ajax]
            [pigeon-scoops.spec.orders :as os]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.forms.groceries :refer [grocery-entry]]
            [pigeon-scoops.forms.recipes :refer [recipe-entry]]
            [pigeon-scoops.forms.flavors :refer [flavor-entry]]
            [pigeon-scoops.forms.orders :refer [order-entry]]
            [pigeon-scoops.auth :refer [authenticator]]
            [pigeon-scoops.components.entry-list :refer [entry-list]]
            [pigeon-scoops.utils :refer [api-url]]
            [uix.core :as uix :refer [$ defui]]
            [uix.dom]
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

(defui app-menu-item [{:keys [text icon app-key set-active-app!]}]
       ($ ListItem
          ($ ListItemButton {:onClick #(set-active-app! app-key)}
             ($ ListItemIcon
                ($ icon))
             ($ ListItemText {:primary text}))))

(defui content []
       (let [[menu-open? set-menu-open!] (uix/use-state false)
             [active-app set-active-app!] (uix/use-state :flavors)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh-groceries? set-refresh-groceries!] (uix/use-state true)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh-recipes? set-refresh-recipes!] (uix/use-state true)
             [flavors set-flavors!] (uix/use-state nil)
             [refresh-flavors? set-refresh-flavors!] (uix/use-state true)
             [orders set-orders!] (uix/use-state nil)
             [refresh-orders? set-refresh-orders!] (uix/use-state true)
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
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "flavors")
                       {:response-format :transit
                        :handler         set-flavors!
                        :error-handler   (fn [_]
                                           (set-flavors! []))}))
           [refresh-flavors?])
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "orders")
                       {:response-format :transit
                        :handler         set-orders!
                        :error-handler   (fn [_]
                                           (set-orders! []))}))
           [refresh-orders?])

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
                                                     ["Recipes" MenuBookIcon :recipes]
                                                     ["Flavors" IcecreamIcon :flavors]
                                                     ["Orders" ReceiptIcon :orders]]]
                    ($ app-menu-item {:key             app-key
                                      :app-key         app-key
                                      :text            app-name
                                      :icon            app-icon
                                      :set-active-app! #(do (set-active-app! %)
                                                            (set-menu-open! (not menu-open?)))}))))
            ($ Box {:component "div"}
               ($ Toolbar)
               ($ entry-list {:title      "Grocery Item"
                              :entries    groceries
                              :entry-form grocery-entry
                              :id-key     ::gs/type
                              :name-key   ::gs/type
                              :sort-key   ::gs/type
                              :endpoint   "groceries"
                              :on-change  #(set-refresh-groceries! (not refresh-groceries?))
                              :active?    (= active-app :groceries)})
               ($ entry-list {:title           "Recipe"
                              :entries         recipes
                              :entry-form      recipe-entry
                              :id-key          ::rs/id
                              :name-key        ::rs/name
                              :sort-key        ::rs/name
                              :endpoint        "recipes"
                              :config-metadata {:groceries groceries}
                              :on-change       #(set-refresh-recipes! (not refresh-recipes?))
                              :active?         (= active-app :recipes)})
               ($ entry-list {:title           "Flavor"
                              :entries         flavors
                              :entry-form      flavor-entry
                              :id-key          ::fs/id
                              :name-key        ::fs/name
                              :sort-key        ::fs/name
                              :endpoint        "flavors"
                              :config-metadata {:recipes recipes}
                              :on-change       #(set-refresh-flavors! (not refresh-flavors?))
                              :active?         (= active-app :flavors)})
               ($ entry-list {:title           "Order"
                              :entries         orders
                              :entry-form      order-entry
                              :id-key          ::os/id
                              :name-key        ::os/note
                              :sort-key        ::os/note
                              :endpoint        "orders"
                              :config-metadata {:flavors flavors :groceries groceries}
                              :on-change       #(set-refresh-orders! (not refresh-orders?))
                              :active?         (= active-app :orders)})))))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ content) root))

(defn ^:export init []
  (render))
