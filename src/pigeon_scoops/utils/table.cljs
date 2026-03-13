(ns pigeon-scoops.utils.table)

(defn make-sorter [key]
  (fn [a b]
    (let [[a b] (map #(js->clj % :keywordize-keys true) [a b])]
      (compare (get a key) (get b key)))))
