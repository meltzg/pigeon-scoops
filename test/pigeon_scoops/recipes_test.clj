(ns pigeon-scoops.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.recipes :as r]
            [pigeon-scoops.units.common :as u]))

(def groceries '(#:grocery{:units
                           [#:grocery{:source           "star market"
                                      :unit-volume      1
                                      :unit-volume-type :volume/gal
                                      :unit-mass        3.9
                                      :unit-mass-type   :mass/kg
                                      :unit-cost        5.0}
                            #:grocery{:source           "star market"
                                      :unit-volume      0.5
                                      :unit-volume-type :volume/gal
                                      :unit-mass        1.95
                                      :unit-mass-type   :mass/kg
                                      :unit-cost        3.5}]
                           :type :grocery/milk}
                  #:grocery{:units
                            [#:grocery{:source           "star market"
                                       :unit-volume      1
                                       :unit-volume-type :volume/qt
                                       :unit-mass        968
                                       :unit-mass-type   :mass/g
                                       :unit-cost        7.5}
                             #:grocery{:source           "star market"
                                       :unit-volume      1
                                       :unit-volume-type :volume/pt
                                       :unit-mass        484
                                       :unit-mass-type   :mass/g
                                       :unit-cost        5.5}]
                            :type :grocery/heavy-cream}
                  #:grocery{:units [] :type :grocery/salt}))

(def recipe-no-id {:recipe/name         "foobar"
                   :recipe/type         :recipe/ice-cream
                   :recipe/instructions ["mix it all together"]
                   :recipe/amount       1
                   :recipe/amount-unit  :volume/qt
                   :recipe/ingredients  [{:recipe/ingredient-type :grocery/milk
                                          :recipe/amount          1
                                          :recipe/amount-unit     :volume/c}
                                         {:recipe/ingredient-type :grocery/heavy-cream
                                          :recipe/amount          2
                                          :recipe/amount-unit     :volume/c}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id :recipe/ingredients [{:recipe/ingredient-type :grocery/milk
                                            :recipe/amount          1
                                            :recipe/amount-unit     :volume/pt}
                                           {:recipe/ingredient-type :grocery/heavy-cream
                                            :recipe/amount          2
                                            :recipe/amount-unit     :volume/pt}
                                           {:recipe/ingredient-type :grocery/salt
                                            :recipe/amount          1
                                            :recipe/amount-unit     :common/pinch}]))

(def recipe-with-id
  (assoc recipe-no-id :recipe/id (java.util.UUID/randomUUID)
                      :recipe/name "fizzbuz"))

(def another-recipe-with-id
  (assoc recipe-no-id :recipe/id (java.util.UUID/randomUUID)
                      :recipe/name "another"))

(deftest add-recipe-test
  (testing "Valid recipes can be added to collection of recipes"
    (are [recipes new-recipe expected]
      (= (set (r/add-recipe recipes new-recipe)) (set expected))
      ;; add recipe to nil collection
      nil recipe-with-id (list recipe-with-id)
      ;; add recipe to empty collection
      [] recipe-with-id [recipe-with-id]
      ;; add recipe to existing collection
      [recipe-with-id] another-recipe-with-id [recipe-with-id another-recipe-with-id]
      ;; add duplicate ID keeps new
      [recipe-with-id] (assoc recipe-with-id :recipe/name "duplicate ID") [(assoc recipe-with-id :recipe/name "duplicate ID")]
      ;; add invalid does not add
      [recipe-with-id] (dissoc another-recipe-with-id :recipe/name) [recipe-with-id])))

(deftest add-recipe-no-id-test
  (testing "A recipe with no ID is assigned one"
    (is (let [added-recipe (first (r/add-recipe nil recipe-no-id))]
          (and (not (contains? recipe-no-id :recipe/id))
               (contains? added-recipe :recipe/id)
               (= (dissoc added-recipe :recipe/id) recipe-no-id))))))

(deftest scale-recipe-test
  (testing "A recipe can be scaled up and down"
    (are [recipe amount amount-unit expected]
      (= (r/scale-recipe recipe amount amount-unit) expected)
      recipe-no-id 3 :volume/qt {:recipe/name         "foobar"
                                 :recipe/type         :recipe/ice-cream
                                 :recipe/instructions ["mix it all together"]
                                 :recipe/amount       3
                                 :recipe/amount-unit  :volume/qt
                                 :recipe/ingredients  [{:recipe/ingredient-type :grocery/milk
                                                        :recipe/amount          3.0
                                                        :recipe/amount-unit     :volume/c}
                                                       {:recipe/ingredient-type :grocery/heavy-cream
                                                        :recipe/amount          6.0
                                                        :recipe/amount-unit     :volume/c}]}
      recipe-no-id 0.5 :volume/qt {:recipe/name         "foobar"
                                   :recipe/type         :recipe/ice-cream
                                   :recipe/instructions ["mix it all together"]
                                   :recipe/amount       0.5
                                   :recipe/amount-unit  :volume/qt
                                   :recipe/ingredients  [{:recipe/ingredient-type :grocery/milk
                                                          :recipe/amount          0.5
                                                          :recipe/amount-unit     :volume/c}
                                                         {:recipe/ingredient-type :grocery/heavy-cream
                                                          :recipe/amount          1.0
                                                          :recipe/amount-unit     :volume/c}]}
      recipe-no-id 4 :volume/c {:recipe/name         "foobar"
                                :recipe/type         :recipe/ice-cream
                                :recipe/instructions ["mix it all together"]
                                :recipe/amount       4
                                :recipe/amount-unit  :volume/c
                                :recipe/ingredients  [{:recipe/ingredient-type :grocery/milk
                                                       :recipe/amount          1.0
                                                       :recipe/amount-unit     :volume/c}
                                                      {:recipe/ingredient-type :grocery/heavy-cream
                                                       :recipe/amount          2.0
                                                       :recipe/amount-unit     :volume/c}]}
      recipe-no-id 2 :volume/l {:recipe/name         "foobar"
                                :recipe/type         :recipe/ice-cream
                                :recipe/instructions ["mix it all together"]
                                :recipe/amount       2
                                :recipe/amount-unit  :volume/l
                                :recipe/ingredients  [{:recipe/ingredient-type :grocery/milk
                                                       :recipe/amount          (* 1 2 (u/convert 1 :volume/l :volume/qt))
                                                       :recipe/amount-unit     :volume/c}
                                                      {:recipe/ingredient-type :grocery/heavy-cream
                                                       :recipe/amount          (* 2 2 (u/convert 1 :volume/l :volume/qt))
                                                       :recipe/amount-unit     :volume/c}]})))

(deftest merge-recipe-ingredients-test
  (testing "a list of ingredients can be made from combining several recipes"
    (are [recipes expected]
      (= (r/merge-recipe-ingredients recipes) expected)
      [recipe-no-id (r/scale-recipe recipe-no-id 2 :volume/qt)] [{:recipe/ingredient-type :grocery/milk
                                                                  :recipe/amount          3.0
                                                                  :recipe/amount-unit     :volume/c}
                                                                 {:recipe/ingredient-type :grocery/heavy-cream
                                                                  :recipe/amount          6.0
                                                                  :recipe/amount-unit     :volume/c}]
      [recipe-no-id recipe-no-id-different-ingredients] [{:recipe/ingredient-type :grocery/milk
                                                          :recipe/amount          3.0
                                                          :recipe/amount-unit     :volume/c}
                                                         {:recipe/ingredient-type :grocery/heavy-cream
                                                          :recipe/amount          6.0
                                                          :recipe/amount-unit     :volume/c}
                                                         {:recipe/ingredient-type :grocery/salt
                                                          :recipe/amount          1
                                                          :recipe/amount-unit     :common/pinch}])))

(deftest to-grocery-purchase-list-test
  (testing "recipe ingredients can be turned into grocery lists"
    (is (= (r/to-grocery-purchase-list
             (r/merge-recipe-ingredients
               [(r/scale-recipe recipe-no-id-different-ingredients 1 :volume/gal)
                recipe-no-id])
             groceries)
           {:purchase-list '(#:grocery{:type  :grocery/milk
                                       :amount-needed 4.5
                                       :grocery/amount-needed-unit :volume/pt
                                       :units (#:grocery{:source                 "star market"
                                                         :unit-cost              5.0
                                                         :unit-mass              3.9
                                                         :unit-mass-type         :mass/kg
                                                         :unit-purchase-quantity 1
                                                         :unit-volume            1
                                                         :unit-volume-type       :volume/gal})}
                              #:grocery{:type  :grocery/heavy-cream
                                        :amount-needed 9.0
                                        :grocery/amount-needed-unit :volume/pt
                                        :units (#:grocery{:source                 "star market"
                                                          :unit-cost              7.5
                                                          :unit-mass              968
                                                          :unit-mass-type         :mass/g
                                                          :unit-purchase-quantity 4
                                                          :unit-volume            1
                                                          :unit-volume-type       :volume/qt}
                                                 #:grocery{:source                 "star market"
                                                           :unit-cost              5.5
                                                           :unit-mass              484
                                                           :unit-mass-type         :mass/g
                                                           :unit-purchase-quantity 1
                                                           :unit-volume            1
                                                           :unit-volume-type       :volume/pt})}
                              #:grocery{:type  :grocery/salt
                                        :amount-needed 4.0
                                        :grocery/amount-needed-unit :common/pinch
                                        :units nil})
            :total-cost    40.5}))))
