(ns pigeon-scoops.api
  (:require [cognitect.transit :as transit]))

(defn get-groceries [token on-success]
  (let [reader (transit/reader :json)]
    (-> (js/fetch "https://api.pigeon-scoops.com/v1/groceries"
                  (clj->js {:method  "GET"
                            :headers {:Accept        "application/transit+json"
                                      :Authorization (str "Bearer " token)}}))
        (.then #(.text %))
        (.then (fn [body]
                 (->> body
                      (transit/read reader)
                      (on-success)))))))

(defn get-grocery [token on-success grocery-id]
  (let [reader (transit/reader :json)]
    (-> (js/fetch (str "https://api.pigeon-scoops.com/v1/groceries/" grocery-id)
                  (clj->js {:method  "GET"
                            :headers {:Accept        "application/transit+json"
                                      :Authorization (str "Bearer " token)}}))
        (.then #(.text %))
        (.then (fn [body]
                 (->> body
                      (transit/read reader)
                      (on-success)))))))
