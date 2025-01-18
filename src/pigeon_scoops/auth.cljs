(ns pigeon-scoops.auth
  (:require [clojure.string :as str]
            [uix.core :as uix :refer [$ defui]]
            ["@auth0/auth0-react" :refer [useAuth0]]
            ["@mui/icons-material/AccountCircle$default" :as AccountCircle]
            ["@mui/material" :refer [IconButton
                                     Menu
                                     MenuItem
                                     Stack
                                     Typography]]))

(defui authenticator []
       (let [[anchor-el set-anchor-el!] (uix/use-state nil)
             {:keys [logout loginWithRedirect isAuthenticated user]} (js->clj (useAuth0) :keywordize-keys true)]
         ($ Stack {:direction "column"}
            ($ IconButton {:size     "large"
                           :on-click #(set-anchor-el! (.-currentTarget %))
                           :color    (if isAuthenticated "default" "error")}
               ($ Typography {:variant "h6"}
                  (:name user))
               ($ AccountCircle))
            ($ Menu {:id           "menu-connection"
                     :anchor-el    anchor-el
                     :keep-mounted true
                     :open         (some? anchor-el)
                     :on-close     #(set-anchor-el! nil)}
               (if isAuthenticated
                 ($ MenuItem {:on-click #(logout (clj->js {:logoutParams {:returnTo (.. js/window -location -origin)}}))}
                    "Sign Out")
                 ($ MenuItem {:on-click loginWithRedirect}
                    "Sign In"))))))
