(ns pigeon-scoops.core
  (:require [uix.core :refer [$ defui]]
            [uix.dom]
            ["@auth0/auth0-react" :refer [Auth0Provider useAuth0]]))

(defui LoginButton []
       (let [{:keys [loginWithRedirect]} (js->clj (useAuth0) :keywordize-keys true)]
         ($ :button {:on-click loginWithRedirect}
            "Login")))

(defui LogoutButton []
       (let [{:keys [logout]} (js->clj (useAuth0) :keywordize-keys true)]
         ($ :button {:on-click #(logout (clj->js {:logoutParams {:returnTo (.. js/window -location -origin)}}))}
            "Logout")))

(defui app []
       ($ Auth0Provider {:domain               "pigeon-scoops.us.auth0.com"
                         :client-id            "AoU9LnGWQlCbSUvjgXdHf4NZPJh0VHYD"
                         :authorization-params (clj->js {:redirect_uri (.. js/window -location -origin)})}
          ($ :h1 "Hello, UIx!")
          ($ LoginButton)
          ($ LogoutButton)))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
