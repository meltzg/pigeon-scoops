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

(deftest scale-ingredient-for-recipe-test
  (testing "An ingredient can be scaled based on recipe scaling up and down as well as across systems of measure"
    (are [amount amount-unit expected] (= (r/scale-ingredient-for-recipe (:recipe/amount-unit recipe-no-id) amount amount-unit (last (:recipe/ingredients recipe-no-id))) expected)
                                       3 :volume/qt {:recipe/ingredient-type :ingredient/heavy-cream
                                                     :recipe/amount          6.0
                                                     :recipe/amount-unit     :volume/c}
                                       0.5 :volume/qt {:recipe/ingredient-type :ingredient/heavy-cream
                                                       :recipe/amount          1.0
                                                       :recipe/amount-unit     :volume/c}
                                       4 :volume/c {:recipe/ingredient-type :ingredient/heavy-cream
                                                    :recipe/amount          2.0
                                                    :recipe/amount-unit     :volume/c}
                                       2 :volume/l {:recipe/ingredient-type :ingredient/heavy-cream
                                                    :recipe/amount          (* 2 2 (u/convert 1 :volume/l :volume/qt))
                                                    :recipe/amount-unit     :volume/c})))

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
                                                                                               :recipe/amount-unit     :volume/c}]})))
