(ns pigeon-scoops.api
  (:require [cognitect.transit :as transit]))

(def base-url "https://api.pigeon-scoops.com/v1")
;(def base-url "http://localhost:8080/v1")

(defn get-constants [token]
  (let [reader (transit/reader :json)]
    (-> (js/fetch (str base-url "/constants")
                  (clj->js {:method  "GET"
                            :headers {:Accept        "application/transit+json"
                                      :Authorization (str "Bearer " token)}}))
        (.then #(.text %))
        (.then (fn [body]
                 (->> body
                      (transit/read reader)))))))

(defn get-groceries [token]
  (let [reader (transit/reader :json)]
    (-> (js/fetch (str base-url "/groceries")
                  (clj->js {:method  "GET"
                            :headers {:Accept        "application/transit+json"
                                      :Authorization (str "Bearer " token)}}))
        (.then #(.text %))
        (.then (fn [body]
                 (->> body
                      (transit/read reader)))))))

(defn get-grocery [token grocery-id]
  (let [reader (transit/reader :json)]
    (-> (js/fetch (str base-url "/groceries/" grocery-id)
                  (clj->js {:method  "GET"
                            :headers {:Accept        "application/transit+json"
                                      :Authorization (str "Bearer " token)}}))
        (.then #(.text %))
        (.then (fn [body]
                 (->> body
                      (transit/read reader)))))))
