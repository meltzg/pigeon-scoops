(ns pigeon-scoops.components.recipe-manager-test
  (:require [clojure.test :as t]
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

(def small-recipe-no-id {::rs/name        "itty bitty"
                         ::rs/type        ::rs/mixin
                         ::rs/amount      0.25
                         ::rs/amount-unit ::vol/tsp
                         ::rs/ingredients [{::rs/ingredient-type ::gs/xanthan-gum
                                            ::rs/amount          0.25
                                            ::rs/amount-unit     ::vol/tsp}]})

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

(t/deftest scale-recipe-test
  (t/testing "A recipe can be scaled up and down"
    (t/are [recipe amount amount-unit expected]
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
                                                   ::rs/amount-unit     ::vol/c}]}
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
                                                     ::rs/amount-unit     ::vol/c}]}
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
                                                  ::rs/amount-unit     ::vol/c}]}
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
                                                  ::rs/amount-unit     ::vol/c}]}
      small-recipe-no-id 0.25 ::vol/tsp small-recipe-no-id)))

(t/deftest merge-recipe-ingredients-test
  (t/testing "a list of ingredients can be made from combining several recipes"
    (t/are [recipes expected]
      (= (rm/merge-recipe-ingredients recipes) expected)
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
                                                          ::rs/amount-unit     ::u/pinch}])))

(t/deftest to-grocery-purchase-list-test
  (t/testing "recipe ingredients can be turned into grocery lists"
    (t/is (= (rm/to-grocery-purchase-list
               (rm/merge-recipe-ingredients
                 [(rm/scale-recipe recipe-no-id-different-ingredients 1 ::vol/gal)
                  recipe-no-id])
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
