(ns pigeon-scoops.auth
  (:require
   ["@ant-design/icons" :refer [UserOutlined]]
   ["@auth0/auth0-react" :refer [useAuth0]]
   [antd :refer [Button Spin]]
   [pigeon-scoops.hooks :refer [base-url use-token]]
   [uix.core :as uix :refer [$ defui]]))

(defui authenticator []
  (let [{:keys [loginWithRedirect isAuthenticated user isLoading] auth-logout :logout} (js->clj (useAuth0) :keywordize-keys true)
        {:keys [token]} (use-token)
        login #(loginWithRedirect
                (clj->js {:authorizationParams
                          {:audience "https://api.pigeon-scoops.com"
                           :scope    "openid profile email offline_access"}}))
        logout #(auth-logout (clj->js {:logoutParams {:returnTo (.. js/window -location -origin)}}))]

    (uix/use-effect
     (fn []
       (when (and isAuthenticated token)
         (-> (js/fetch (str base-url "/account")
                       (clj->js {:method :POST
                                 :headers {:Accept "application/transit+json"
                                           :Authorization (str "Bearer " token)}}))
             (.then #(when-not (.-ok %)
                       (js/Promise.reject %)))
             (.catch #(do
                        (js/alert (str "Login failed." %))
                        (logout))))))
     [isAuthenticated token logout])

    ($ Button {:type "primary"
               :on-click (if (and isAuthenticated token)
                           logout
                           login)
               :icon-placement "end"
               :icon ($ UserOutlined {:style {:color (cond isLoading "orange"
                                                           (and isAuthenticated token) "black"
                                                           :else "red")}})}
       (cond isLoading ($ Spin)
             (and isAuthenticated token) (:name user)
             :else "Sign In"))))
