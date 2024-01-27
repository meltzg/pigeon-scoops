(ns pigeon-scoops.components.amount-config
  (:require [uix.core :as uix :refer [$ defui]]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            ["@mui/material" :refer [FormControl
                                     InputLabel
                                     MenuItem
                                     Select
                                     Stack
                                     TextField]]))

(defui amount-config [{:keys [entry on-change set-valid! entry-namespace default-amount-unit accepted-unit-types]}]
       (let [kw (partial keyword entry-namespace)
             amount-valid? #(and (re-matches #"^\d+\.?\d*$" (str ((kw "amount") entry)))
                                 (s/valid? (kw "amount") (js/parseFloat ((kw "amount") entry))))
             amount-unit-valid? #(s/valid? (kw "amount-unit") ((kw "amount-unit") entry))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace (or ((kw "amount-unit") entry)
                                                                                    default-amount-unit)))]
         (uix/use-effect
           (fn []
             (cond (true? (:reset entry)) (set-amount-unit-type! (namespace (or ((kw "amount-unit") entry)
                                                                                default-amount-unit)))
                   (or (not ((kw "amount-unit") entry)) (not= amount-unit-type (namespace ((kw "amount-unit") entry))))
                   (on-change
                     (assoc entry (kw "amount-unit")
                                  (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                        (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))
                                        (= amount-unit-type (namespace ::ucom/pinch)) (first ucom/other-units))))))
           [default-amount-unit kw entry amount-unit-type on-change])
         (uix/use-effect
           (fn []
             (set-valid! (and amount-valid?
                              amount-unit-valid?)))
           [set-valid! amount-valid? amount-unit-valid?])

         ($ Stack {:direction "column" :spacing 1.25}
            ($ TextField {:label     "Amount"
                          :error     (not (amount-valid?))
                          :value     (or ((kw "amount") entry) 0)
                          :on-change #(on-change (assoc entry (kw "amount") (js/parseFloat (.. % -target -value))))})
            ($ FormControl {:full-width true}
               ($ InputLabel "Amount type")
               ($ Select {:value     amount-unit-type
                          :on-change #(set-amount-unit-type! (.. % -target -value))}
                  (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                       (map namespace accepted-unit-types))))
            ($ FormControl {:full-width true
                            :error      (not (amount-unit-valid?))}
               ($ InputLabel "Amount unit")
               ($ Select {:value     (or ((kw "amount-unit") entry)
                                         (first (keys volume/conversion-map)))
                          :on-change #(on-change (assoc entry (kw "amount-unit") (keyword amount-unit-type (.. % -target -value))))}
                  (map #($ MenuItem {:value % :key %} (name %))
                       (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                             (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))
                             (= amount-unit-type (namespace ::ucom/pinch)) (set ucom/other-units))))))))
