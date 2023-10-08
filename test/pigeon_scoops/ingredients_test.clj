(ns pigeon-scoops.ingredients-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.ingredients :as i]))

(def ingredient
  {:ingredient/type        :ingredient/milk
   :ingredient/description "moo moo juice"
   :ingredient/units       [{:ingredient/source           "dark market"
                             :ingredient/unit-cost        6.5
                             :ingredient/unit-volume      1.0
                             :ingredient/unit-volume-type :volume/gal}
                            {:ingredient/source           "dark market"
                             :ingredient/unit-cost        3.25
                             :ingredient/unit-mass        1.95
                             :ingredient/unit-mass-type   :mass/kg}]})

(def another-ingredient
  (assoc ingredient :ingredient/type :ingredient/heavy-cream
                    :ingredient/description "heavy moo moo juice"))

(deftest add-ingredient-test
  (testing "Valid ingredients can be added to collection of ingredients"
    (are [ingredients new-ingredient expected] (= (set (i/add-ingredient ingredients new-ingredient)) (set expected))
                                               ;; add ingredient to nil collection
                                               nil ingredient (list ingredient)
                                               ;; add ingredient to empty collection
                                               [] ingredient [ingredient]
                                               ;; add ingredient to existing collection
                                               [ingredient] another-ingredient [ingredient another-ingredient]
                                               ;; add duplicate type keeps new
                                               [ingredient] (assoc ingredient :ingredient/description "duplicate type") [(assoc ingredient :ingredient/description "duplicate type")]
                                               ;; add invalid does not add
                                               [ingredient] (dissoc another-ingredient :ingredient/type) [ingredient])))
