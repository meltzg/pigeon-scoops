(ns pigeon-scoops.hooks
  (:require [uix.core :as uix :refer [defhook]]
            ["@auth0/auth0-react" :refer [useAuth0]]))

(defhook use-token []
         (let [{:keys [getAccessTokenSilently isAuthenticated]} (js->clj (useAuth0) :keywordize-keys true)
               [token set-token!] (uix/use-state nil)]
           (uix/use-effect
             (fn []
               (when isAuthenticated
                 (-> (getAccessTokenSilently (clj->js {:authorizationParams {:audience "https://api.pigeon-scoops.com"
                                                                             :scope    "openid profile email offline_access"}}))
                     (.then set-token!))))
             [getAccessTokenSilently isAuthenticated])
           {:token token}))
