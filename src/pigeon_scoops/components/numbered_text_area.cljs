(ns pigeon-scoops.components.numbered-text-area
  (:require [clojure.string :as str]
            [uix.core :refer [$ defui]]
            ["@mui/material" :refer [TextField]]))

(defui numbered-text-area [{:keys [lines set-lines!]}]
       ($ TextField {:value     (str/join "\n" (map-indexed #(str (inc %1) ") " %2) lines))
                     :on-change #(set-lines! (let [new-lines (mapv (fn [line]
                                                                     (str/replace line #"^\d+?\)\s*" ""))
                                                                   (str/split-lines (.. % -target -value)))]
                                               (if (str/ends-with? (.. % -target -value) "\n")
                                                 (conj new-lines "")
                                                 new-lines)))
                     :on-key-up #(if (and (str/blank? (last lines))
                                          (= (.-key %) "Backspace"))
                                   (set-lines! (butlast lines)))
                     :label     "Instructions"
                     :multiline true}))

