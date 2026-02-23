(ns pigeon-scoops.utils
  (:require
   [clojure.set :as set]
   [clojure.string :as str]))

(defn determine-ops [id-key original-entities updated-entities]
  (let [original-ids (set (map id-key original-entities))
        updated-ids (set (map id-key updated-entities))]
    (as-> updated-entities $
      (group-by #(if (original-ids (id-key %)) :update :new) $)
      (update $ :update (partial remove #((set original-entities) %)))
      (assoc $ :delete (set/difference original-ids updated-ids)))))

(defn stringify-keyword [k]
  (if (keyword? k)
    (str k)
    k))

(defn parse-keyword [s]
  (when s (keyword (if (str/starts-with? s ":")
                     (subs s 1)
                     s))))

(defn deep-stringify-keyword-vals [data]
  (cond
    (keyword? data) (stringify-keyword data)
    (map? data) (into {} (map (fn [[k v]] [k (deep-stringify-keyword-vals v)])) data)
    (coll? data) (map deep-stringify-keyword-vals data)
    :else data))
