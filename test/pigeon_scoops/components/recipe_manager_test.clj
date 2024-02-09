(ns pigeon-scoops.components.recipe-manager-test
  (:require [clojure.test :as t]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]))

(def groceries '(#::gs{:units
                       [#::gs{:source           "star market"
                              :unit-volume      1
                              :unit-volume-type ::volume/gal
                              :unit-mass        3.9
                              :unit-mass-type   ::mass/kg
                              :unit-cost        5.0}
                        #::gs{:source           "star market"
                              :unit-volume      0.5
                              :unit-volume-type ::volume/gal
                              :unit-mass        1.95
                              :unit-mass-type   ::mass/kg
                              :unit-cost        3.5}]
                       :type ::gs/milk}
                  #::gs{:units
                        [#::gs{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::volume/qt
                               :unit-mass        968
                               :unit-mass-type   ::mass/g
                               :unit-cost        7.5}
                         #::gs{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::volume/pt
                               :unit-mass        484
                               :unit-mass-type   ::mass/g
                               :unit-cost        5.5}]
                        :type ::gs/heavy-cream}
                  #::gs{:units [] :type ::gs/salt}))

(def recipe-no-id {::rs/name         "foobar"
                   ::rs/type         ::rs/ice-cream
                   ::rs/instructions ["mix it all together"]
                   ::rs/amount       1
                   ::rs/amount-unit  ::volume/qt
                   ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                       ::rs/amount          1
                                       ::rs/amount-unit     ::volume/c}
                                      {::rs/ingredient-type ::gs/heavy-cream
                                       ::rs/amount          2
                                       ::rs/amount-unit     ::volume/c}]})

(def metric-recipe-no-id {::rs/name         "foobar"
                          ::rs/type         ::rs/ice-cream
                          ::rs/instructions ["mix it all together"]
                          ::rs/amount       1
                          ::rs/amount-unit  ::volume/qt
                          ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                              ::rs/amount          100
                                              ::rs/amount-unit     ::mass/g}
                                             {::rs/ingredient-type ::gs/heavy-cream
                                              ::rs/amount          200
                                              ::rs/amount-unit     ::mass/g}]})

(def small-recipe-no-id {::rs/name        "itty bitty"
                         ::rs/type        ::rs/mixin
                         ::rs/amount      0.25
                         ::rs/amount-unit ::volume/tsp
                         ::rs/ingredients [{::rs/ingredient-type ::gs/xanthan-gum
                                            ::rs/amount          0.25
                                            ::rs/amount-unit     ::volume/tsp}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id ::rs/ingredients [{::rs/ingredient-type ::gs/milk
                                         ::rs/amount          1
                                         ::rs/amount-unit     ::volume/pt}
                                        {::rs/ingredient-type ::gs/heavy-cream
                                         ::rs/amount          2
                                         ::rs/amount-unit     ::volume/pt}
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
      recipe-no-id 3 ::volume/qt {::rs/name         "foobar"
                                  ::rs/type         ::rs/ice-cream
                                  ::rs/instructions ["mix it all together"]
                                  ::rs/amount       3
                                  ::rs/amount-unit  ::volume/qt
                                  ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                      ::rs/amount          3.0
                                                      ::rs/amount-unit     ::volume/c}
                                                     {::rs/ingredient-type ::gs/heavy-cream
                                                      ::rs/amount          6.0
                                                      ::rs/amount-unit     ::volume/c}]}
      recipe-no-id 0.5 ::volume/qt {::rs/name         "foobar"
                                    ::rs/type         ::rs/ice-cream
                                    ::rs/instructions ["mix it all together"]
                                    ::rs/amount       0.5
                                    ::rs/amount-unit  ::volume/qt
                                    ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                        ::rs/amount          0.5
                                                        ::rs/amount-unit     ::volume/c}
                                                       {::rs/ingredient-type ::gs/heavy-cream
                                                        ::rs/amount          1.0
                                                        ::rs/amount-unit     ::volume/c}]}
      recipe-no-id 4 ::volume/c {::rs/name         "foobar"
                                 ::rs/type         ::rs/ice-cream
                                 ::rs/instructions ["mix it all together"]
                                 ::rs/amount       4
                                 ::rs/amount-unit  ::volume/c
                                 ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                     ::rs/amount          1.0
                                                     ::rs/amount-unit     ::volume/c}
                                                    {::rs/ingredient-type ::gs/heavy-cream
                                                     ::rs/amount          2.0
                                                     ::rs/amount-unit     ::volume/c}]}
      recipe-no-id 2 ::volume/l {::rs/name         "foobar"
                                 ::rs/type         ::rs/ice-cream
                                 ::rs/instructions ["mix it all together"]
                                 ::rs/amount       2
                                 ::rs/amount-unit  ::volume/l
                                 ::rs/ingredients  [{::rs/ingredient-type ::gs/milk
                                                     ::rs/amount          (* 1 2 (u/convert 1 ::volume/l ::volume/qt))
                                                     ::rs/amount-unit     ::volume/c}
                                                    {::rs/ingredient-type ::gs/heavy-cream
                                                     ::rs/amount          (* 2 2 (u/convert 1 ::volume/l ::volume/qt))
                                                     ::rs/amount-unit     ::volume/c}]}
      small-recipe-no-id 0.25 ::volume/tsp small-recipe-no-id)))

(t/deftest merge-recipe-ingredients-test
  (t/testing "a list of ingredients can be made from combining several recipes"
    (t/are [recipes expected]
      (= (rm/merge-recipe-ingredients recipes groceries) expected)
      [recipe-no-id (rm/scale-recipe recipe-no-id 2 ::volume/qt)] [{::rs/ingredient-type ::gs/milk
                                                                    ::rs/amount          3.0
                                                                    ::rs/amount-unit     ::volume/c}
                                                                   {::rs/ingredient-type ::gs/heavy-cream
                                                                    ::rs/amount          6.0
                                                                    ::rs/amount-unit     ::volume/c}]
      [recipe-no-id recipe-no-id-different-ingredients] [{::rs/ingredient-type ::gs/milk
                                                          ::rs/amount          3.0
                                                          ::rs/amount-unit     ::volume/c}
                                                         {::rs/ingredient-type ::gs/heavy-cream
                                                          ::rs/amount          6.0
                                                          ::rs/amount-unit     ::volume/c}
                                                         {::rs/ingredient-type ::gs/salt
                                                          ::rs/amount          1
                                                          ::rs/amount-unit     ::u/pinch}]
      [recipe-no-id metric-recipe-no-id] [{::rs/ingredient-type ::gs/milk
                                           ::rs/amount          1.4102564102564104
                                           ::rs/amount-unit     ::volume/c}
                                          {::rs/ingredient-type ::gs/heavy-cream
                                           ::rs/amount          2.8264462809917354
                                           ::rs/amount-unit     ::volume/c}])))

(t/deftest to-grocery-purchase-list-test
  (t/testing "recipe ingredients can be turned into grocery lists"
    (t/is (= (rm/to-grocery-purchase-list
               (rm/merge-recipe-ingredients
                 [(rm/scale-recipe recipe-no-id-different-ingredients 1 ::volume/gal)
                  recipe-no-id]
                 groceries)
               groceries)
             {:purchase-list       '(#::gs{:type                 ::gs/milk
                                           :amount-needed        4.5
                                           :amount-needed-cost   2.8125
                                           :amount-needed-unit   ::volume/pt
                                           :purchase-amount      1
                                           :purchase-amount-unit ::volume/gal
                                           :purchase-cost        5.0
                                           :units                (#::gs{:source                 "star market"
                                                                        :unit-cost              5.0
                                                                        :unit-mass              3.9
                                                                        :unit-mass-type         ::mass/kg
                                                                        :unit-purchase-quantity 1
                                                                        :unit-volume            1
                                                                        :unit-volume-type       ::volume/gal})}
                                      #::gs{:type                 ::gs/heavy-cream
                                            :amount-needed        9.0
                                            :amount-needed-cost   35.5
                                            :amount-needed-unit   ::volume/pt
                                            :purchase-amount      4.5
                                            :purchase-amount-unit ::volume/qt
                                            :purchase-cost        35.5
                                            :units                (#::gs{:source                 "star market"
                                                                         :unit-cost              7.5
                                                                         :unit-mass              968
                                                                         :unit-mass-type         ::mass/g
                                                                         :unit-purchase-quantity 4
                                                                         :unit-volume            1
                                                                         :unit-volume-type       ::volume/qt}
                                                                    #::gs{:source                 "star market"
                                                                          :unit-cost              5.5
                                                                          :unit-mass              484
                                                                          :unit-mass-type         ::mass/g
                                                                          :unit-purchase-quantity 1
                                                                          :unit-volume            1
                                                                          :unit-volume-type       ::volume/pt})}
                                      #::gs{:type                 ::gs/salt
                                            :amount-needed        4.0
                                            :amount-needed-cost   nil
                                            :amount-needed-unit   ::u/pinch
                                            :purchase-amount      nil
                                            :purchase-amount-unit nil
                                            :purchase-cost        nil
                                            :units                nil})
              :total-purchase-cost 40.5
              :total-needed-cost   38.3125}))))

(t/deftest can-merge-recipe-ingredients?-test
  (t/testing "recipe ingredients can be merged if their types match and their amount units are mass or volume"
    (t/are [recipe-ingredients expected]
      (= (rm/can-merge-recipe-ingredients? recipe-ingredients) expected)
      [#:pigeon-scoops.spec.recipes{:ingredient-type ::gs/milk,
                                    :amount          0.265625,
                                    :amount-unit     ::volume/c}
       #:pigeon-scoops.spec.recipes{:ingredient-type ::gs/milk,
                                    :amount          1615.0,
                                    :amount-unit     ::mass/g}] true
      [#:pigeon-scoops.spec.recipes{:ingredient-type ::gs/milk,
                                    :amount          0.265625,
                                    :amount-unit     ::volume/c}
       #:pigeon-scoops.spec.recipes{:ingredient-type ::gs/milk,
                                    :amount          1615.0,
                                    :amount-unit     ::u/pinch}] false
      [#:pigeon-scoops.spec.recipes{:ingredient-type ::gs/milk,
                                    :amount          0.265625,
                                    :amount-unit     ::volume/c}
       #:pigeon-scoops.spec.recipes{:ingredient-type ::gs/honey,
                                    :amount          1615.0,
                                    :amount-unit     ::mass/g}] false)))

(t/deftest apply-grocery-unit-test
  (t/testing "a recipe ingredient can be converted to the units in a grocery unit"
    (t/are [recipe-ingredient expected]
      (= (rm/apply-grocery-unit recipe-ingredient
                                {::gs/unit-volume-type ::volume/gal,
                                 ::gs/source           "Market Basket",
                                 ::gs/unit-mass-type   ::mass/kg,
                                 ::gs/unit-volume      1.0,
                                 ::gs/unit-mass        3.9,
                                 ::gs/unit-cost        2.59})
         expected)
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          1.0,
       ::rs/amount-unit     ::volume/c}
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          0.0625,
       ::rs/amount-unit     ::volume/gal}
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          1615.0,
       ::rs/amount-unit     ::mass/g}
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          1.615,
       ::rs/amount-unit     ::mass/kg}
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          3.14,
       ::rs/amount-unit     ::u/pinch}
      {::rs/ingredient-type ::gs/milk,
       ::rs/amount          3.14,
       ::rs/amount-unit     ::u/pinch})))

(t/deftest change-unit-type
  (t/testing "a recipe ingredient can have it's unit changed from mass to volume given a conversion unit"
    (t/are [to-type recipe-ingredient expected]
      (= (rm/change-unit-type to-type
                              recipe-ingredient
                              {::gs/unit-mass        2.0
                               ::gs/unit-mass-type   ::mass/kg
                               ::gs/unit-volume      3.0
                               ::gs/unit-volume-type ::volume/ml})
         expected)
      (namespace ::mass/g) {::rs/type        ::gs/milk
                            ::rs/amount      3
                            ::rs/amount-unit ::volume/ml} {::rs/type        ::gs/milk
                                                           ::rs/amount      2.0
                                                           ::rs/amount-unit ::mass/kg}
      (namespace ::volume/ml) {::rs/type        ::gs/milk
                               ::rs/amount      2
                               ::rs/amount-unit ::mass/kg} {::rs/type        ::gs/milk
                                                            ::rs/amount      3.0
                                                            ::rs/amount-unit ::volume/ml}
      (namespace ::volume/ml) {::rs/type        ::gs/milk
                               ::rs/amount      2
                               ::rs/amount-unit ::mass/g} {::rs/type        ::gs/milk
                                                           ::rs/amount      (/ 3.0 1000)
                                                           ::rs/amount-unit ::volume/ml}
      (namespace ::mass/lb) {::rs/type        ::gs/milk
                             ::rs/amount      2
                             ::rs/amount-unit ::mass/g} {::rs/type        ::gs/milk
                                                         ::rs/amount      2
                                                         ::rs/amount-unit ::mass/g})))
