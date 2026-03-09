(ns pigeon-scoops.auth
  (:require
   ["@ant-design/icons" :refer [UserOutlined]]
   ["@auth0/auth0-react" :refer [useAuth0]]
   [antd :refer [Dropdown Space]]
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

    ($ Dropdown
       {:menu (clj->js {:onClick #(let [key (:key (js->clj % :keywordize-keys true))]
                                    (case key
                                      "1" nil
                                      "2" (logout)
                                      "3" (login)))
                        :items (if (and isAuthenticated token)
                                 [{:key "1" :label (:name user)}
                                  {:key "2" :label ($ :span {:style {:display "block" :width "100%" :text-align "right"}}
                                                      "Sign Out")}]
                                 [{:key "3" :label "Sign In"}])})}
       ($ :a {:on-click #(.preventDefault %)}
          ($ Space
             "Account"
             ($ UserOutlined {:style {:color (cond isLoading "orange"
                                                   isAuthenticated "white"
                                                   :else "red")}}))))))