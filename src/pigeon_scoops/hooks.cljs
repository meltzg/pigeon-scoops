(ns pigeon-scoops.hooks
  (:require
   ["@auth0/auth0-react" :refer [useAuth0]]
   ["swr$default" :as useSWR]
   [swr :refer [mutate]]
   [pigeon-scoops.fetchers :refer [get-fetcher!]]
   [pigeon-scoops.utils :refer [stringify-keyword]]
   [uix.core :as uix :refer [defhook]]
   [clojure.string :as str]))

(def base-url
  (if ^boolean goog.DEBUG
    "http://localhost:8080/v1"                              ;; Development URL
    "https://api.pigeon-scoops.com/v1"))                    ;; Production URL


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
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/constants") token]
                         (fn [[url]]
                           (get-fetcher! url {:token token
                                              :headers {"Accept" "application/transit+json"}})))
                 :keywordize-keys true)]
    {:constants data
     :error     error
     :loading?  isLoading}))

(defhook use-recipes []
  (let [{:keys [token]} (use-token)
        opts (cond-> {:headers {"Accept" "application/transit+json"}}
               token (assoc :token token))
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/recipes") token]
                         (fn [[url]]
                           (get-fetcher! url opts)))
                 :keywordize-keys true)]
    {:recipes data
     :error   error
     :loading? isLoading}))

(defhook use-recipe [recipe-id scaled-amount scaled-amount-unit]
  (let [{:keys [token]} (use-token)
        query-params (when scaled-amount
                       (js/URLSearchParams.
                        (clj->js {:amount scaled-amount
                                  :amount-unit (stringify-keyword scaled-amount-unit)})))
        opts (cond-> {:headers {"Accept" "application/transit+json"}}
               token (assoc :token token))
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/recipes/" recipe-id "?" (or query-params "")) token]
                         (fn [[url]]
                           (when (uuid? recipe-id)
                             (get-fetcher! url opts))))
                 :keywordize-keys true)]
    {:recipe data
     :error   error
     :loading? isLoading}))

(defhook use-recipe-bom [recipe-id scaled-amount scaled-amount-unit]
  (let [{:keys [token]} (use-token)
        query-params (js/URLSearchParams.
                      (clj->js {:amount scaled-amount
                                :amount-unit (stringify-keyword scaled-amount-unit)}))
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/recipes/" recipe-id "/bom?" (or query-params "")) token]
                         (fn [[url]]
                           (when (and recipe-id scaled-amount scaled-amount-unit token)
                             (get-fetcher! url {:token token
                                                :headers {"Accept" "application/transit+json"}}))))
                 :keywordize-keys true)]
    {:groceries data
     :error   error
     :loading? isLoading}))

(defn invalidate-recipes []
  (mutate (fn [key]
            (str/starts-with? (first key) (str base-url "/recipes")))))

(defhook use-groceries []
  (let [{:keys [token]} (use-token)
        opts (cond-> {:headers {"Accept" "application/transit+json"}}
               token (assoc :token token))
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/groceries") token]
                         (fn [[url]]
                           (get-fetcher! url opts)))
                 :keywordize-keys true)]
    {:groceries data
     :error     error
     :loading?  isLoading}))

(defhook use-grocery [grocery-id]
  (let [{:keys [token]} (use-token)
        opts (cond-> {:headers {"Accept" "application/transit+json"}}
               token (assoc :token token))
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/groceries/" grocery-id) token]
                         (fn [[url]]
                           (when (uuid? grocery-id)
                             (get-fetcher! url opts))))
                 :keywordize-keys true)]
    {:grocery data
     :error   error
     :loading? isLoading}))

(defn invalidate-groceries []
  (mutate (fn [key]
            (str/starts-with? (first key) (str base-url "/groceries")))))

(defhook use-orders []
  (let [{:keys [token]} (use-token)
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/orders") token]
                         (fn [[url]]
                           (when token
                             (get-fetcher! url {:token token
                                                :headers {"Accept" "application/transit+json"}}))))
                 :keywordize-keys true)]
    {:orders data
     :error     error
     :loading?  isLoading}))

(defhook use-order [order-id]
  (let [{:keys [token]} (use-token)
        {:keys [data error isLoading]}
        (js->clj (useSWR [(str base-url "/orders/" order-id) token]
                         (fn [[url]]
                           (when (and (uuid? order-id) token)
                             (get-fetcher! url {:token token
                                                :headers {"Accept" "application/transit+json"}}))))
                 :keywordize-keys true)]
    {:order data
     :error   error
     :loading? isLoading}))

(defn invalidate-orders []
  (mutate (fn [key]
            (str/starts-with? (first key) (str base-url "/orders")))))
