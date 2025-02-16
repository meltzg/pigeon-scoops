(ns pigeon-scoops.api
  (:require [cognitect.transit :as transit]))

(defn make-request [{:keys [method url body token]}]
  (let [base-url "https://api.pigeon-scoops.com/v1"
        reader (transit/reader :json)
        writer (transit/writer :json)]
    (-> (js/fetch (str base-url url)
                  (clj->js (cond-> {:method  method
                                    :headers (cond-> {:Accept "application/transit+json"}
                                                     body (assoc :Content-Type "application/transit+json")
                                                     token (assoc :Authorization (str "Bearer " token)))}
                                   body (assoc :body (transit/write writer body)))))
        (.then #(.text %))
        (.then (partial transit/read reader)))))

(defn get-constants []
  (make-request {:method "GET" :url "/constants"}))

(defn get-groceries [token]
  (make-request {:method "GET" :url "/groceries" :token token}))

(defn get-grocery [token grocery-id]
  (make-request {:method "GET" :url (str "/groceries/" grocery-id) :token token}))

(defn get-recipes [token]
  (make-request {:method "GET" :url "/recipes" :token token}))

(defn get-recipe [token recipe-id {:keys [amount amount-unit]}]
  (let [query-params (when amount
                       (js/URLSearchParams.
                         (clj->js {:amount      amount
                                   :amount-unit (str (namespace amount-unit) "/" (name amount-unit))})))]
    (make-request {:method "GET" :url (str "/recipes/" recipe-id "?" (or query-params "")) :token token})))

(defn get-orders [token]
  (make-request {:method "GET" :url "/orders" :token token}))

(defn get-order [token order-id]
  (make-request {:method "GET" :url (str "/orders/" order-id) :token token}))
