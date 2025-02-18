(ns pigeon-scoops.api
  (:require [cognitect.transit :as transit]))

(defn make-request [{:keys [method url body token]}]
  (let [base-url "https://api.pigeon-scoops.com/v1"
        base-url "http://localhost:8080/v1"
        reader (transit/reader :json)
        writer (transit/writer :json)]
    (-> (js/fetch (str base-url url)
                  (clj->js (cond-> {:method  (name method)
                                    :headers (cond-> {:Accept "application/transit+json"}
                                                     body (assoc :Content-Type "application/transit+json")
                                                     token (assoc :Authorization (str "Bearer " token)))}
                                   body (assoc :body (transit/write writer (update-keys body (comp keyword name)))))))
        (.then #(if (.-ok %)
                  %
                  (js/Promise.reject %)))
        (.then #(.text %))
        (.then (partial transit/read reader)))))

(defn get-constants []
  (make-request {:method :GET :url "/constants"}))

(defn get-groceries [token]
  (make-request {:method :GET :url "/groceries" :token token}))

(defn get-grocery [token grocery-id]
  (make-request {:method :GET :url (str "/groceries/" grocery-id) :token token}))

(defn create-grocery [token grocery]
  (make-request {:method :POST :url "/groceries" :token token :body grocery}))

(defn update-grocery [token {:grocery/keys [id] :as grocery}]
  (make-request {:method :PUT :url (str "/groceries/" id) :token token :body grocery}))

(defn delete-grocery [token grocery-id]
  (make-request {:method :DELETE :url (str "/groceries/" grocery-id) :token token}))

(defn create-grocery-unit [token grocery-id grocery-unit]
  (make-request {:method :POST :url (str "/groceries/" grocery-id "/units") :body grocery-unit :token token}))

(defn update-grocery-unit [token grocery-id grocery-unit]
  (make-request {:method :PUT :url (str "/groceries/" grocery-id "/units") :body grocery-unit :token token}))

(defn delete-grocery-unit [token grocery-id grocery-unit-id]
  (make-request {:method :POST :url (str "/groceries/" grocery-id "/units") :body {:id grocery-unit-id} :token token}))

(defn get-recipes [token]
  (make-request {:method :GET :url "/recipes" :token token}))

(defn get-recipe [token recipe-id {:keys [amount amount-unit]}]
  (let [query-params (when amount
                       (js/URLSearchParams.
                         (clj->js {:amount      amount
                                   :amount-unit (str (namespace amount-unit) "/" (name amount-unit))})))]
    (make-request {:method :GET :url (str "/recipes/" recipe-id "?" (or query-params "")) :token token})))

(defn get-orders [token]
  (make-request {:method :GET :url "/orders" :token token}))

(defn get-order [token order-id]
  (make-request {:method :GET :url (str "/orders/" order-id) :token token}))
