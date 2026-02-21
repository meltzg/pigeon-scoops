(ns pigeon-scoops.fetchers
  (:require [cognitect.transit :as transit]))

(defn encode-body [content-type body]
  (cond
    (= content-type "application/transit+msgpack")
    (let [writer (transit/writer :msgpack)]
      (transit/write writer body))
    (= content-type "application/transit+json")
    (let [writer (transit/writer :json)]
      (transit/write writer body))
    (= content-type "application/json")
    (js/JSON.stringify (clj->js body))
    :else
    (throw (ex-info "Unsupported content type" {:content-type content-type}))))

(defn decode-body [content-type body]
  (prn "Decoding body with content type:" content-type "and body:" body)
  (cond
    (= content-type "application/transit+msgpack")
    (let [reader (transit/reader :msgpack)]
      (transit/read reader body))
    (= content-type "application/transit+json")
    (let [reader (transit/reader :json)]
      (transit/read reader body))
    (= content-type "application/json")
    (js->clj (js/JSON.parse body) :keywordize-keys true)
    :else
    (throw (ex-info "Unsupported content type" {:content-type content-type}))))

(defn base-fetcher! [url method {:keys [token headers body]}]
  (let [headers (merge headers
                       (cond-> {"Authorization" (str "Bearer " token)}
                         (and body (nil? (get headers "Content-Type"))) (assoc "Content-Type" "application/json")
                         (nil? (get headers "Accept")) (assoc "Accept" "application/json")))
        method (.toUpperCase (name method))
        request-options (clj->js (cond-> {:method method
                                          :headers headers}
                                   body (assoc :body (encode-body (get headers "Content-Type") body))))
        response-promise (js/fetch url request-options)]
    (.then response-promise
           (fn [response]
             (cond
               (not (.-ok response))
               (throw (ex-info "Fetch error" {:status (.-status response)
                                              :status-text (.-statusText response)}))
               (and (not= (.-status response) 204) (not= (-> response .-headers (.get "Content-Length")) "0"))
               (let [content-type (-> response .-headers (.get "Content-Type") (.split ";") (first) (.trim))]
                 (-> response
                     (.text)
                     (.then (partial decode-body content-type))))
               :else
               (do
                 (prn "No content to decode for response with status:" (.-status response))
                 nil))))))

(defn get-fetcher! [url opts]
  (base-fetcher! url :get opts))

(defn post-fetcher! [url opts]
  (base-fetcher! url :post opts))

(defn put-fetcher! [url opts]
  (base-fetcher! url :put opts))

(defn delete-fetcher! [url opts]
  (base-fetcher! url :delete opts))
