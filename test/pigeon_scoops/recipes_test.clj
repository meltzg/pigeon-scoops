(ns pigeon-scoops.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.recipes :as r]))

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

(deftest add-recipe-no-id
  (testing "A recipe with no ID is assigned one"
    (is (let [added-recipe (first (r/add-recipe nil recipe-no-id))]
          (and (not (contains? recipe-no-id :recipe/id))
               (contains? added-recipe :recipe/id)
               (= (dissoc added-recipe :recipe/id) recipe-no-id))))))
