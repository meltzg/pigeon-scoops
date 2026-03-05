(ns pigeon-scoops.utils
  (:require
   [clojure.set :as set]))

(defn determine-ops
  ([id-key original-entities updated-entities]
   (determine-ops id-key original-entities updated-entities identity))
  ([id-key original-entities updated-entities make-comparable]
   (let [original-ids (set (map id-key original-entities))
         updated-ids (set (map id-key updated-entities))]
     (as-> (map make-comparable updated-entities) $
       (group-by #(if (original-ids (id-key %)) :update :new) $)
       (update $ :update (partial remove #((set (map make-comparable original-entities)) %)))
       (assoc $ :delete (set/difference original-ids updated-ids))))))

(defn stringify-keyword [k]
  (if (keyword? k)
    (.substring (str k) 1)
    k))

(defn parse-keyword [s]
  (when s (keyword s)))

(defn make-sorter [key]
  (fn [a b]
    (let [[a b] (map #(js->clj % :keywordize-keys true) [a b])]
      (compare (get a key) (get b key)))))
