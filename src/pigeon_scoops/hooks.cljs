(ns pigeon-scoops.hooks
  (:require
   ["@auth0/auth0-react" :refer [useAuth0]]
   [pigeon-scoops.api :refer [base-url]]
   [pigeon-scoops.fetchers :refer [get-fetcher!]]
   ["swr$default" :as useSWR]
   [uix.core :as uix :refer [defhook]]))

(defhook use-token []
  (let [{:keys [getAccessTokenSilently isAuthenticated]} (js->clj (useAuth0) :keywordize-keys true)
        [token set-token!] (uix/use-state nil)
        [loading? set-loading!] (uix/use-state true)]
    (uix/use-effect
     (fn []
       (set-loading! true)
       (if isAuthenticated
         (-> (getAccessTokenSilently (clj->js {:authorizationParams {:audience "https://api.pigeon-scoops.com"
                                                                     :scope    "openid profile email offline_access"}}))
             (.then (juxt set-token! (partial set-loading! false))))
         (set-loading! false)))
     [getAccessTokenSilently isAuthenticated])
    {:token    token
     :loading? loading?}))

(defhook use-constants []
  (let [{:keys [token]} (use-token)
        {:keys [data error isLoading]} (js->clj (useSWR [(str base-url "/constants") token]
                                                        (fn [[url]]
                                                          (get-fetcher! url {:token token
                                                                             :headers {"Accept" "application/transit+json"}})))
                                                :keywordize-keys true)]
    (prn "Constants updated:" data error isLoading)
    {:constants data
     :error     error
     :loading?  isLoading}))
