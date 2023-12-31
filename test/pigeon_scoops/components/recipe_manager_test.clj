(ns pigeon-scoops.components.recipe-manager-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def groceries '(#::gs{:units
                       [#::gs{:source           "star market"
                              :unit-volume      1
                              :unit-volume-type ::vol/gal
                              :unit-mass        3.9
                              :unit-mass-type   ::mass/kg
                              :unit-cost        5.0}
                        #::gs{:source           "star market"
                              :unit-volume      0.5
                              :unit-volume-type ::vol/gal
                              :unit-mass        1.95
                              :unit-mass-type   ::mass/kg
                              :unit-cost        3.5}]
                       :type ::gs/milk}
                  #::gs{:units
                        [#::gs{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::vol/qt
                               :unit-mass        968
                               :unit-mass-type   ::mass/g
                               :unit-cost        7.5}
                         #::gs{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::vol/pt
                               :unit-mass        484
                               :unit-mass-type   ::mass/g
                               :unit-cost        5.5}]
                        :type ::gs/heavy-cream}
                  #::gs{:units [] :type ::gs/salt}))

(def recipe-no-id {::rs/name         "foobar"
                   ::rs/type         ::rs/ice-cream
                   ::rs/instructions ["mix it all together"]
                   ::rs/amount       1
                   ::rs/amount-unit  ::vol/qt
                   ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                       ::rs/amount          1
                                       ::rs/amount-unit     ::vol/c}
                                      {::rs/ingredient-type ::gs/heavy-cream
                                       ::rs/amount          2
                                       ::rs/amount-unit     ::vol/c}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id ::rs/ingredients [{::rs/ingredient-type ::gs/milk
                                         ::rs/amount          1
                                         ::rs/amount-unit     ::vol/pt}
                                        {::rs/ingredient-type ::gs/heavy-cream
                                         ::rs/amount          2
                                         ::rs/amount-unit     ::vol/pt}
                                        {::rs/ingredient-type ::gs/salt
                                         ::rs/amount          1
                                         ::rs/amount-unit     ::u/pinch}]))

(def recipe-with-id
  (assoc recipe-no-id ::rs/id (java.util.UUID/randomUUID)
                      ::rs/name "fizzbuz"))

(def another-recipe-with-id
  (assoc recipe-no-id ::rs/id (java.util.UUID/randomUUID)
                      ::rs/name "another"))

(def recipe-mixins [{::rs/id           #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                     ::rs/name         "crushed oreo"
                     ::rs/type         ::rs/mixin
                     ::rs/instructions ["crush 'em"]
                     ::rs/amount       0.5
                     ::rs/amount-unit  ::vol/c
                     ::rs/ingredients  [{::rs/ingredient-type ::gs/oreo
                                         ::rs/amount          12
                                         ::rs/amount-unit     ::u/unit}]}
                    {::rs/id           #uuid"dc5edc8e-79cf-4601-aab6-778d4897106a"
                     ::rs/name         "oreo milk"
                     ::rs/type         ::rs/mixin
                     ::rs/instructions ["more"]
                     ::rs/amount       1
                     ::rs/amount-unit  ::vol/c
                     ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                         ::rs/amount          1
                                         ::rs/amount-unit     ::vol/c}]
                     ::rs/mixins       [{::rs/id          #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                                         ::rs/amount      0.25
                                         ::rs/amount-unit ::vol/c}]}])

(def recipe-no-id-with-mixins
  (assoc recipe-no-id ::rs/mixins (map #(hash-map ::rs/id (::rs/id %)
                                                  ::rs/amount (* 0.5 (::rs/amount %))
                                                  ::rs/amount-unit (::rs/amount-unit %)) recipe-mixins)))

(deftest get-recipes-test
  (testing "Recipes can be retrieved by ID"
    (are [ids expected]
      (= (set (apply (partial rm/get-recipes {::rm/recipes (atom [recipe-with-id another-recipe-with-id])}) ids)) (set expected))
      [] [recipe-with-id another-recipe-with-id]
      [(::rs/id recipe-with-id)] [recipe-with-id]
      [(::rs/id recipe-with-id) (::rs/id another-recipe-with-id)] [recipe-with-id another-recipe-with-id]
      [(random-uuid)] [])))

(deftest add-recipe-test
  (testing "Valid recipes can be added to collection of recipes"
    (are [recipes new-recipe expected]
      (= (set (rm/add-recipe recipes new-recipe)) (set expected))
      ;; add recipe to nil collection
      nil recipe-with-id (list recipe-with-id)
      ;; add recipe to empty collection
      [] recipe-with-id [recipe-with-id]
      ;; add recipe to existing collection
      [recipe-with-id] another-recipe-with-id [recipe-with-id another-recipe-with-id]
      ;; add duplicate ID keeps new
      [recipe-with-id] (assoc recipe-with-id ::rs/name "duplicate ID") [(assoc recipe-with-id ::rs/name "duplicate ID")]
      ;; add invalid does not add
      [recipe-with-id] (dissoc another-recipe-with-id ::rs/name) [recipe-with-id])))

(deftest add-recipe-no-id-test
  (testing "A recipe with no ID is assigned one"
    (is (let [added-recipe (first (rm/add-recipe nil recipe-no-id))]
          (and (not (contains? recipe-no-id ::rs/id))
               (contains? added-recipe ::rs/id)
               (= (dissoc added-recipe ::rs/id) recipe-no-id))))))

(deftest materialize-mixins-test
  (testing "Mixins can recursively be materialized"
    (is (= (rm/materialize-mixins recipe-no-id-with-mixins recipe-mixins)
           '(#::rs{:amount       0.25
                   :amount-unit  ::vol/c
                   :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                   :ingredients  (#::rs{:amount          1.5
                                        :amount-unit     ::u/unit
                                        :ingredient-type ::gs/oreo})
                   :instructions ["crush 'em"]
                   :mixins       ()
                   :name         "crushed oreo"
                   :type         ::rs/mixin}
              #::rs{:amount       0.5
                    :amount-unit  ::vol/c
                    :id           #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"
                    :ingredients  (#::rs{:amount          0.5
                                         :amount-unit     ::vol/c
                                         :ingredient-type ::gs/milk})
                    :instructions ["more"]
                    :mixins       (#::rs{:amount      0.125
                                         :amount-unit ::vol/c
                                         :id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"})
                    :name         "oreo milk"
                    :type         ::rs/mixin}
              #::rs{:amount       0.125
                    :amount-unit  ::vol/c
                    :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                    :ingredients  (#::rs{:amount          0.75
                                         :amount-unit     ::u/unit
                                         :ingredient-type ::gs/oreo})
                    :instructions ["crush 'em"]
                    :mixins       ()
                    :name         "crushed oreo"
                    :type         ::rs/mixin})))))

(deftest scale-recipe-test
  (testing "A recipe can be scaled up and down"
    (are [recipe amount amount-unit expected]
      (= (rm/scale-recipe recipe amount amount-unit) expected)
      recipe-no-id 3 ::vol/qt {::rs/name         "foobar"
                               ::rs/type         ::rs/ice-cream
                               ::rs/instructions ["mix it all together"]
                               ::rs/amount       3
                               ::rs/amount-unit  ::vol/qt
                               ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                   ::rs/amount          3.0
                                                   ::rs/amount-unit     ::vol/c}
                                                  {::rs/ingredient-type ::gs/heavy-cream
                                                   ::rs/amount          6.0
                                                   ::rs/amount-unit     ::vol/c}]
                               ::rs/mixins       []}
      recipe-no-id 0.5 ::vol/qt {::rs/name         "foobar"
                                 ::rs/type         ::rs/ice-cream
                                 ::rs/instructions ["mix it all together"]
                                 ::rs/amount       0.5
                                 ::rs/amount-unit  ::vol/qt
                                 ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                     ::rs/amount          0.5
                                                     ::rs/amount-unit     ::vol/c}
                                                    {::rs/ingredient-type ::gs/heavy-cream
                                                     ::rs/amount          1.0
                                                     ::rs/amount-unit     ::vol/c}]
                                 ::rs/mixins       []}
      recipe-no-id 4 ::vol/c {::rs/name         "foobar"
                              ::rs/type         ::rs/ice-cream
                              ::rs/instructions ["mix it all together"]
                              ::rs/amount       4
                              ::rs/amount-unit  ::vol/c
                              ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                  ::rs/amount          1.0
                                                  ::rs/amount-unit     ::vol/c}
                                                 {::rs/ingredient-type ::gs/heavy-cream
                                                  ::rs/amount          2.0
                                                  ::rs/amount-unit     ::vol/c}]
                              ::rs/mixins       []}
      recipe-no-id 2 ::vol/l {::rs/name         "foobar"
                              ::rs/type         ::rs/ice-cream
                              ::rs/instructions ["mix it all together"]
                              ::rs/amount       2
                              ::rs/amount-unit  ::vol/l
                              ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                  ::rs/amount          (* 1 2 (u/convert 1 ::vol/l ::vol/qt))
                                                  ::rs/amount-unit     ::vol/c}
                                                 {::rs/ingredient-type ::gs/heavy-cream
                                                  ::rs/amount          (* 2 2 (u/convert 1 ::vol/l ::vol/qt))
                                                  ::rs/amount-unit     ::vol/c}]
                              ::rs/mixins       []}
      recipe-no-id-with-mixins 3 ::vol/qt {::rs/name         "foobar"
                                           ::rs/type         ::rs/ice-cream
                                           ::rs/instructions ["mix it all together"]
                                           ::rs/amount       3
                                           ::rs/amount-unit  ::vol/qt
                                           ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                               ::rs/amount          3.0
                                                               ::rs/amount-unit     ::vol/c}
                                                              {::rs/ingredient-type ::gs/heavy-cream
                                                               ::rs/amount          6.0
                                                               ::rs/amount-unit     ::vol/c}]
                                           ::rs/mixins       [{::rs/amount      0.75
                                                               ::rs/amount-unit ::vol/c
                                                               ::rs/id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
                                                              {::rs/amount      1.5
                                                               ::rs/amount-unit ::vol/c
                                                               ::rs/id          #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"}]})))

(deftest merge-recipe-ingredients-test
  (testing "a list of ingredients can be made from combining several recipes"
    (are [recipes expected]
      (= (rm/merge-recipe-ingredients recipes (concat recipe-mixins recipes)) expected)
      [recipe-no-id (rm/scale-recipe recipe-no-id 2 ::vol/qt)] [{::rs/ingredient-type ::gs/milk
                                                                 ::rs/amount          3.0
                                                                 ::rs/amount-unit     ::vol/c}
                                                                {::rs/ingredient-type ::gs/heavy-cream
                                                                 ::rs/amount          6.0
                                                                 ::rs/amount-unit     ::vol/c}]
      [recipe-no-id recipe-no-id-different-ingredients] [{::rs/ingredient-type ::gs/milk
                                                          ::rs/amount          3.0
                                                          ::rs/amount-unit     ::vol/c}
                                                         {::rs/ingredient-type ::gs/heavy-cream
                                                          ::rs/amount          6.0
                                                          ::rs/amount-unit     ::vol/c}
                                                         {::rs/ingredient-type ::gs/salt
                                                          ::rs/amount          1
                                                          ::rs/amount-unit     ::u/pinch}]
      [recipe-no-id-with-mixins (rm/scale-recipe recipe-no-id-with-mixins 2 ::vol/qt)] [{::rs/ingredient-type ::gs/oreo
                                                                                         ::rs/amount          6.75
                                                                                         ::rs/amount-unit     ::u/unit}
                                                                                        {::rs/ingredient-type ::gs/milk
                                                                                         ::rs/amount          4.5
                                                                                         ::rs/amount-unit     ::vol/c}
                                                                                        {::rs/ingredient-type ::gs/heavy-cream
                                                                                         ::rs/amount          6.0
                                                                                         ::rs/amount-unit     ::vol/c}])))

(deftest to-grocery-purchase-list-test
  (testing "recipe ingredients can be turned into grocery lists"
    (is (= (rm/to-grocery-purchase-list
             (rm/merge-recipe-ingredients
               [(rm/scale-recipe recipe-no-id-different-ingredients 1 ::vol/gal)
                recipe-no-id]
               [recipe-no-id])
             groceries)
           {:purchase-list '(#::gs{:type                   ::gs/milk
                                   :amount-needed          4.5
                                   ::gs/amount-needed-unit ::vol/pt
                                   :units                  (#::gs{:source                 "star market"
                                                                  :unit-cost              5.0
                                                                  :unit-mass              3.9
                                                                  :unit-mass-type         ::mass/kg
                                                                  :unit-purchase-quantity 1
                                                                  :unit-volume            1
                                                                  :unit-volume-type       ::vol/gal})}
                              #::gs{:type                   ::gs/heavy-cream
                                    :amount-needed          9.0
                                    ::gs/amount-needed-unit ::vol/pt
                                    :units                  (#::gs{:source                 "star market"
                                                                   :unit-cost              7.5
                                                                   :unit-mass              968
                                                                   :unit-mass-type         ::mass/g
                                                                   :unit-purchase-quantity 4
                                                                   :unit-volume            1
                                                                   :unit-volume-type       ::vol/qt}
                                                              #::gs{:source                 "star market"
                                                                    :unit-cost              5.5
                                                                    :unit-mass              484
                                                                    :unit-mass-type         ::mass/g
                                                                    :unit-purchase-quantity 1
                                                                    :unit-volume            1
                                                                    :unit-volume-type       ::vol/pt})}
                              #::gs{:type                   ::gs/salt
                                    :amount-needed          4.0
                                    ::gs/amount-needed-unit ::u/pinch
                                    :units                  nil})
            :total-cost    40.5}))))
