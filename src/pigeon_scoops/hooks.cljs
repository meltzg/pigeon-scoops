(ns pigeon-scoops.hooks
  (:require [uix.core :as uix :refer [defhook]]
            ["@auth0/auth0-react" :refer [useAuth0]]))

(defhook use-token []
         (let [{:keys [getAccessTokenSilently]} (js->clj (useAuth0) :keywordize-keys true)
               [token set-token!] (uix/use-state nil)]
           (uix/use-effect
             (fn []
               (-> (getAccessTokenSilently (clj->js {:authorizationParams {:audience "https://api.pigeon-scoops.com"}}))
                   (.then set-token!)))
             [getAccessTokenSilently])
           {:token token}))
