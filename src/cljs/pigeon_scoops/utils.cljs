(ns pigeon-scoops.utils
  (:require [clojure.string :as string]
            [uix.core :as uix]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/v1/"))

(defn drop-nth [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn use-validation [initial-value valid-pred]
  (let [[value set-value!] (uix.core/use-state initial-value)
        [valid? set-valid!] (uix.core/use-state (valid-pred initial-value))
        on-change #(let [v (.. % -target -value)]
                     (set-value! v)
                     (set-valid! (valid-pred v)))]
    [value valid? on-change]))