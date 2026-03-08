(ns pigeon-scoops.auth
  (:require
   ["@ant-design/icons" :refer [UserOutlined]]
   ["@auth0/auth0-react" :refer [useAuth0]]
   [antd :refer [Button Popconfirm Spin]]
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

    (if (and isAuthenticated token)
      ($ Popconfirm {:title "Sign out"
                     :description "Are you sure you want to sign out?"
                     :on-confirm logout
                     :ok-text "Yes"
                     :cancel-text "No"}
         ($ Button {:type "primary"
                    :icon-placement "end"
                    :icon ($ UserOutlined {:style {:color "black"}})}
            (if isLoading
              ($ Spin)
              (:name user))))
      ($ Button {:type "primary"
                 :on-click login
                 :icon-placement "end"
                 :icon ($ UserOutlined {:style {:color (if isLoading
                                                         "orange"
                                                         "red")}})}
         "Sign in"))))
