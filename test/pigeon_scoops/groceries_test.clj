(ns pigeon-scoops.groceries-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.groceries :as i]))

(def grocery-item
  {:grocery/type        :grocery/milk
   :grocery/description "moo moo juice"
   :grocery/units       [{:grocery/source           "dark market"
                          :grocery/unit-cost        6.5
                          :grocery/unit-volume      1.0
                          :grocery/unit-volume-type :volume/gal}
                         {:grocery/source         "dark market"
                          :grocery/unit-cost      3.25
                          :grocery/unit-mass      1.95
                          :grocery/unit-mass-type :mass/kg}]})

(def another-grocery-item
  (assoc grocery-item :grocery/type :grocery/heavy-cream
                      :grocery/description "heavy moo moo juice"))

(deftest add-ingredient-test
  (testing "Valid ingredients can be added to collection of ingredients"
    (are [ingredients new-ingredient expected] (= (set (i/add-grocery-item ingredients new-ingredient)) (set expected))
                                               ;; add grocery item to nil collection
                                               nil grocery-item (list grocery-item)
                                               ;; add grocery item to empty collection
                                               [] grocery-item [grocery-item]
                                               ;; add grocery item to existing collection
                                               [grocery-item] another-grocery-item [grocery-item another-grocery-item]
                                               ;; add duplicate type keeps new
                                               [grocery-item] (assoc grocery-item :grocery/description "duplicate type") [(assoc grocery-item :grocery/description "duplicate type")]
                                               ;; add invalid does not add
                                               [grocery-item] (dissoc another-grocery-item :grocery/type) [grocery-item])))
