(ns pigeon-scoops.auth
  (:require [pigeon-scoops.hooks :refer [use-token]]
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
             {:keys [logout loginWithRedirect isAuthenticated user isLoading]} (js->clj (useAuth0) :keywordize-keys true)
             {:keys [token]} (use-token)]
         ($ Stack {:direction "column"}
            ($ IconButton {:size     "large"
                           :on-click #(set-anchor-el! (.-currentTarget %))
                           :color    (cond isLoading "warning"
                                           (and isAuthenticated token) "default"
                                           :else "error")}
               ($ Stack {:direction "row" :spacing 1}
                  ($ Typography {:variant "h6"}
                     (:name user))
                  ($ AccountCircle)))
            ($ Menu {:id           "menu-connection"
                     :anchor-el    anchor-el
                     :keep-mounted true
                     :open         (some? anchor-el)
                     :on-close     #(set-anchor-el! nil)}
               (if (and isAuthenticated token)
                 ($ MenuItem {:on-click #(logout (clj->js {:logoutParams {:returnTo (.. js/window -location -origin)}}))}
                    "Sign Out")
                 ($ MenuItem {:on-click #(loginWithRedirect (clj->js {:authorizationParams {:audience "https://api.pigeon-scoops.com"
                                                                                            :scope    "openid profile email offline_access"}}))}
                    "Sign In"))))))
