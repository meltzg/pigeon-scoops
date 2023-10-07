(ns pigeon-scoops.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.recipes :as r]
            [pigeon-scoops.units.common :as u]))

(def recipe-no-id {:recipe/name         "foobar"
                   :recipe/type         :recipe/ice-cream
                   :recipe/instructions ["mix it all together"]
                   :recipe/amount       1
                   :recipe/amount-unit  :volume/qt
                   :recipe/ingredients  [{:recipe/ingredient-type :ingredient/milk
                                          :recipe/amount          1
                                          :recipe/amount-unit     :volume/c}
                                         {:recipe/ingredient-type :ingredient/heavy-cream
                                          :recipe/amount          2
                                          :recipe/amount-unit     :volume/c}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id :recipe/ingredients [{:recipe/ingredient-type :ingredient/milk
                                            :recipe/amount          1
                                            :recipe/amount-unit     :volume/pt}
                                           {:recipe/ingredient-type :ingredient/heavy-cream
                                            :recipe/amount          2
                                            :recipe/amount-unit     :volume/pt}
                                           {:recipe/ingredient-type :ingredient/salt
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
    (are [recipes new-recipe expected] (= (set (r/add-recipe recipes new-recipe)) (set expected))
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
    (are [recipe amount amount-unit expected] (= (r/scale-recipe recipe amount amount-unit) expected)
                                              recipe-no-id 3 :volume/qt {:recipe/name         "foobar"
                                                                         :recipe/type         :recipe/ice-cream
                                                                         :recipe/instructions ["mix it all together"]
                                                                         :recipe/amount       3
                                                                         :recipe/amount-unit  :volume/qt
                                                                         :recipe/ingredients  [{:recipe/ingredient-type :ingredient/milk
                                                                                                :recipe/amount          3.0
                                                                                                :recipe/amount-unit     :volume/c}
                                                                                               {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                                :recipe/amount          6.0
                                                                                                :recipe/amount-unit     :volume/c}]}
                                              recipe-no-id 0.5 :volume/qt {:recipe/name         "foobar"
                                                                           :recipe/type         :recipe/ice-cream
                                                                           :recipe/instructions ["mix it all together"]
                                                                           :recipe/amount       0.5
                                                                           :recipe/amount-unit  :volume/qt
                                                                           :recipe/ingredients  [{:recipe/ingredient-type :ingredient/milk
                                                                                                  :recipe/amount          0.5
                                                                                                  :recipe/amount-unit     :volume/c}
                                                                                                 {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                                  :recipe/amount          1.0
                                                                                                  :recipe/amount-unit     :volume/c}]}
                                              recipe-no-id 4 :volume/c {:recipe/name         "foobar"
                                                                        :recipe/type         :recipe/ice-cream
                                                                        :recipe/instructions ["mix it all together"]
                                                                        :recipe/amount       4
                                                                        :recipe/amount-unit  :volume/c
                                                                        :recipe/ingredients  [{:recipe/ingredient-type :ingredient/milk
                                                                                               :recipe/amount          1.0
                                                                                               :recipe/amount-unit     :volume/c}
                                                                                              {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                               :recipe/amount          2.0
                                                                                               :recipe/amount-unit     :volume/c}]}
                                              recipe-no-id 2 :volume/l {:recipe/name         "foobar"
                                                                        :recipe/type         :recipe/ice-cream
                                                                        :recipe/instructions ["mix it all together"]
                                                                        :recipe/amount       2
                                                                        :recipe/amount-unit  :volume/l
                                                                        :recipe/ingredients  [{:recipe/ingredient-type :ingredient/milk
                                                                                               :recipe/amount          (* 1 2 (u/convert 1 :volume/l :volume/qt))
                                                                                               :recipe/amount-unit     :volume/c}
                                                                                              {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                               :recipe/amount          (* 2 2 (u/convert 1 :volume/l :volume/qt))
                                                                                               :recipe/amount-unit     :volume/c}]})))

(deftest merge-recipe-ingredients-test
  (testing "a list of ingredients can be made from combining several recipes"
    (are [recipes expected] (= (r/merge-recipe-ingredients recipes) expected)
                            [recipe-no-id (r/scale-recipe recipe-no-id 2 :volume/qt)] [{:recipe/ingredient-type :ingredient/milk
                                                                                        :recipe/amount          3.0
                                                                                        :recipe/amount-unit     :volume/c}
                                                                                       {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                        :recipe/amount          6.0
                                                                                        :recipe/amount-unit     :volume/c}]
                            [recipe-no-id recipe-no-id-different-ingredients] [{:recipe/ingredient-type :ingredient/milk
                                                                                :recipe/amount          3.0
                                                                                :recipe/amount-unit     :volume/c}
                                                                               {:recipe/ingredient-type :ingredient/heavy-cream
                                                                                :recipe/amount          6.0
                                                                                :recipe/amount-unit     :volume/c}
                                                                               {:recipe/ingredient-type :ingredient/salt
                                                                                :recipe/amount          1
                                                                                :recipe/amount-unit     :common/pinch}])))
