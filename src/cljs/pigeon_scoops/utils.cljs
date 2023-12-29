(ns pigeon-scoops.utils
  (:require [clojure.string :as string]
            [uix.core]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/v1/"))

(defn drop-nth [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn use-validation [initial-value valid-pred]
  (let [[value set-value!] (uix.core/use-state initial-value)
        [valid? set-valid!] (uix.core/use-state (valid-pred initial-value))
        on-change #(let [v (try
                             (.. % -target -value)
                             (catch js/Error e
                               %))]
                     (set-value! v)
                     (set-valid! (valid-pred v)))]
    [value valid? on-change]))

(defn error-handler [set-message! {:keys [status status-text]}]
  (set-message! (str "Error: " status " Message: " status-text)))

(defn handle-alert-close [on-close event reason]
  (when (not= reason "clickaway")
    (on-close)))
