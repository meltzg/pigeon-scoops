(ns pigeon-scoops.utils.transform)

(defn stringify-keyword [k]
  (if (keyword? k)
    (.substring (str k) 1)
    k))

(defn parse-keyword [s]
  (when s (keyword s)))
