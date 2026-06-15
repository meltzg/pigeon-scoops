(ns pigeon-scoops.recipe.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.recipe.forms :as forms]))

(def recipe-id (random-uuid))
(def ingredient-id (random-uuid))
(def grocery-id (random-uuid))
(def recipe-ingredient-id (random-uuid))

(def api-recipe
  {:recipe/id recipe-id
   :recipe/name "Vanilla"
   :recipe/is-mystery false
   :recipe/source "Cookbook"
   :recipe/description "A classic"
   :recipe/mystery-description nil
   :recipe/amount 4
   :recipe/amount-unit :unit/cup
   :recipe/instructions ["Mix" "Bake"]
   :recipe/ingredients [{:ingredient/id ingredient-id
                         :ingredient/amount 2
                         :ingredient/amount-unit :unit/cup
                         :ingredient/ingredient-grocery-id grocery-id}]})

(deftest recipe-data->form-values-test
  (testing "stringifies keys/keyword-valued fields and derives ingredient-id options for antd Form consumption"
    (is (= {"recipe/id" recipe-id
            "recipe/name" "Vanilla"
            "recipe/is-mystery" false
            "recipe/source" "Cookbook"
            "recipe/description" "A classic"
            "recipe/mystery-description" nil
            "recipe/amount" 4
            "recipe/amount-unit" "unit/cup"
            "recipe/instructions" "Mix\nBake"
            "recipe/ingredients" [{"ingredient/id" ingredient-id
                                   "ingredient/amount" 2
                                   "ingredient/amount-unit" "unit/cup"
                                   "ingredient/ingredient-grocery-id" grocery-id
                                   "ingredient/ingredient-id" (str "grocery:" grocery-id)}]}
           (forms/recipe-data->form-values api-recipe)))))

(deftest recipe-form-values->data-test
  (testing "parses Form values into API-shaped data, keeping the derived ingredient selector id"
    (is (= (assoc-in api-recipe [:recipe/ingredients 0 :ingredient/ingredient-id]
                     (str "grocery:" grocery-id))
           (forms/recipe-form-values->data
            #js {"recipe/id" recipe-id
                 "recipe/name" "Vanilla"
                 "recipe/is-mystery" false
                 "recipe/source" "Cookbook"
                 "recipe/description" "A classic"
                 "recipe/mystery-description" nil
                 "recipe/amount" 4
                 "recipe/amount-unit" "unit/cup"
                 "recipe/instructions" "Mix\nBake"
                 "recipe/ingredients" #js [#js {"ingredient/id" ingredient-id
                                                "ingredient/amount" 2
                                                "ingredient/amount-unit" "unit/cup"
                                                "ingredient/ingredient-id" (str "grocery:" grocery-id)
                                                "ingredient/ingredient-grocery-id" grocery-id}]}))))

  (testing "splits multi-line instruction text into a vector of steps"
    (is (= ["Step one" "Step two"]
           (:recipe/instructions
            (forms/recipe-form-values->data
             #js {"recipe/instructions" "Step one\nStep two"
                  "recipe/ingredients" #js []})))))

  (testing "treats blank instruction text as no instructions"
    (is (= []
           (:recipe/instructions
            (forms/recipe-form-values->data
             #js {"recipe/instructions" ""
                  "recipe/ingredients" #js []})))))

  (testing "leaves instructions that are already a collection untouched"
    (is (= ["Already" "split"]
           (:recipe/instructions
            (forms/recipe-form-values->data
             {:recipe/instructions ["Already" "split"]
              :recipe/ingredients []}))))))

(deftest ingredient->comparable-test
  (testing "narrows an ingredient to the fields relevant for change detection"
    (is (= {:ingredient/amount 2
            :ingredient/amount-unit :unit/cup
            :ingredient/ingredient-grocery-id grocery-id}
           (forms/ingredient->comparable
            #js {"ingredient/id" ingredient-id
                 "ingredient/amount" 2
                 "ingredient/amount-unit" "unit/cup"
                 "ingredient/ingredient-id" (str "grocery:" grocery-id)
                 "ingredient/ingredient-grocery-id" grocery-id}))))

  (testing "includes additional keys (e.g. :ingredient/id) when requested"
    (is (= {:ingredient/id ingredient-id
            :ingredient/amount 2
            :ingredient/amount-unit :unit/cup
            :ingredient/ingredient-grocery-id grocery-id}
           (forms/ingredient->comparable
            #js {"ingredient/id" ingredient-id
                 "ingredient/amount" 2
                 "ingredient/amount-unit" "unit/cup"
                 "ingredient/ingredient-id" (str "grocery:" grocery-id)
                 "ingredient/ingredient-grocery-id" grocery-id}
            [:ingredient/id])))))

(deftest recipe->comparable-test
  (testing "produces an equal comparable form for equivalent API data and form values"
    (is (= (forms/recipe->comparable api-recipe)
           (forms/recipe->comparable
            #js {"recipe/id" recipe-id
                 "recipe/name" "Vanilla"
                 "recipe/is-mystery" false
                 "recipe/source" "Cookbook"
                 "recipe/description" "A classic"
                 "recipe/mystery-description" nil
                 "recipe/amount" 4
                 "recipe/amount-unit" "unit/cup"
                 "recipe/instructions" "Mix\nBake"
                 "recipe/ingredients" #js [#js {"ingredient/id" ingredient-id
                                                "ingredient/amount" 2
                                                "ingredient/amount-unit" "unit/cup"
                                                "ingredient/ingredient-id" (str "grocery:" grocery-id)
                                                "ingredient/ingredient-grocery-id" grocery-id}]}))))

  (testing "excludes identifying fields like :recipe/id and :ingredient/id from comparison"
    (is (not (contains? (forms/recipe->comparable api-recipe) :recipe/id)))
    (is (not (contains? (first (:recipe/ingredients (forms/recipe->comparable api-recipe)))
                        :ingredient/id)))))

(deftest recipe-save-ops-test
  (testing "splits ingredients into new/update/delete relative to the initial recipe"
    (let [updated-recipe-ingredient {:ingredient/id ingredient-id
                                     :ingredient/amount 3
                                     :ingredient/amount-unit :unit/cup
                                     :ingredient/ingredient-grocery-id grocery-id}
          new-ingredient {:ingredient/amount 1
                          :ingredient/amount-unit :unit/each
                          :ingredient/ingredient-recipe-id recipe-ingredient-id}
          {:keys [recipe ingredient-ops]}
          (forms/recipe-save-ops api-recipe
                                 #js {"recipe/id" recipe-id
                                      "recipe/name" "Vanilla"
                                      "recipe/is-mystery" false
                                      "recipe/source" "Cookbook"
                                      "recipe/description" "A classic"
                                      "recipe/mystery-description" nil
                                      "recipe/amount" 4
                                      "recipe/amount-unit" "unit/cup"
                                      "recipe/instructions" "Mix\nBake"
                                      "recipe/ingredients"
                                      #js [#js {"ingredient/id" ingredient-id
                                                "ingredient/amount" 3
                                                "ingredient/amount-unit" "unit/cup"
                                                "ingredient/ingredient-id" (str "grocery:" grocery-id)
                                                "ingredient/ingredient-grocery-id" grocery-id}
                                           #js {"ingredient/amount" 1
                                                "ingredient/amount-unit" "unit/each"
                                                "ingredient/ingredient-id" (str "recipe:" recipe-ingredient-id)
                                                "ingredient/ingredient-recipe-id" recipe-ingredient-id}]})]
      (is (= recipe-id (:recipe/id recipe)))
      (is (= [new-ingredient] (:new ingredient-ops)))
      (is (= [updated-recipe-ingredient] (:update ingredient-ops)))
      (is (= #{} (:delete ingredient-ops)))))

  (testing "reports removed ingredient ids in :delete"
    (let [{:keys [ingredient-ops]}
          (forms/recipe-save-ops api-recipe
                                 #js {"recipe/id" recipe-id
                                      "recipe/name" "Vanilla"
                                      "recipe/is-mystery" false
                                      "recipe/source" "Cookbook"
                                      "recipe/description" "A classic"
                                      "recipe/mystery-description" nil
                                      "recipe/amount" 4
                                      "recipe/amount-unit" "unit/cup"
                                      "recipe/instructions" "Mix\nBake"
                                      "recipe/ingredients" #js []})]
      (is (empty? (:new ingredient-ops)))
      (is (empty? (:update ingredient-ops)))
      (is (= #{ingredient-id} (:delete ingredient-ops))))))
