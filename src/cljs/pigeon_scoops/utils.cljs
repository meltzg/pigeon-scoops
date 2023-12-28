(ns pigeon-scoops.utils
  (:require [clojure.string :as string]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/v1/"))

(defn drop-nth [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))
