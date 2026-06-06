(ns pigeon-scoops.components.ingredients-selector-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.components.ingredients-selector :as selector]))

(def ingredient-keys
  {:grocery :ingredient/ingredient-grocery-id
   :recipe :ingredient/ingredient-recipe-id})

(def grocery-uuid (random-uuid))
(def recipe-uuid (random-uuid))

(deftest stringify-ingredient-test
  (testing "joins the type and id with a colon"
    (is (= (str "grocery:" grocery-uuid)
           (selector/stringify-ingredient :grocery grocery-uuid)))
    (is (= (str "recipe:" recipe-uuid)
           (selector/stringify-ingredient :recipe recipe-uuid)))))

(deftest ingredient->option-test
  (testing "prefers the recipe id when both are present"
    (is (= (str "recipe:" recipe-uuid)
           (selector/ingredient->option ingredient-keys
                                        {:ingredient/ingredient-recipe-id recipe-uuid
                                         :ingredient/ingredient-grocery-id grocery-uuid}))))
  (testing "falls back to the grocery id"
    (is (= (str "grocery:" grocery-uuid)
           (selector/ingredient->option ingredient-keys
                                        {:ingredient/ingredient-grocery-id grocery-uuid}))))
  (testing "returns nil when neither id is present"
    (is (nil? (selector/ingredient->option ingredient-keys {})))))

(deftest parse-ingredient-test
  (testing "parses a grocery-typed option string into the grocery id key"
    (is (= {:ingredient/ingredient-id (str "grocery:" grocery-uuid)
            :ingredient/ingredient-grocery-id grocery-uuid}
           (selector/parse-ingredient :ingredient/ingredient-id
                                      ingredient-keys
                                      {:ingredient/ingredient-id (str "grocery:" grocery-uuid)}))))
  (testing "parses a recipe-typed option string into the recipe id key"
    (is (= {:ingredient/ingredient-id (str "recipe:" recipe-uuid)
            :ingredient/ingredient-recipe-id recipe-uuid}
           (selector/parse-ingredient :ingredient/ingredient-id
                                      ingredient-keys
                                      {:ingredient/ingredient-id (str "recipe:" recipe-uuid)}))))
  (testing "passes the ingredient through unchanged when the id is absent"
    (is (= {:ingredient/amount 1}
           (selector/parse-ingredient :ingredient/ingredient-id
                                      ingredient-keys
                                      {:ingredient/amount 1}))))
  (testing "throws on an unrecognized ingredient type"
    (is (thrown? ExceptionInfo
                 (selector/parse-ingredient :ingredient/ingredient-id
                                            ingredient-keys
                                            {:ingredient/ingredient-id (str "bogus:" grocery-uuid)})))))
