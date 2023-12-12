(ns pigeon-scoops.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.recipes :as r]
            [pigeon-scoops.groceries :as g]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def groceries '(#::g{:units
                      [#::g{:source           "star market"
                            :unit-volume      1
                            :unit-volume-type ::vol/gal
                            :unit-mass        3.9
                            :unit-mass-type   ::mass/kg
                            :unit-cost        5.0}
                       #::g{:source           "star market"
                            :unit-volume      0.5
                            :unit-volume-type ::vol/gal
                            :unit-mass        1.95
                            :unit-mass-type   ::mass/kg
                            :unit-cost        3.5}]
                      :type ::g/milk}
                  #::g{:units
                       [#::g{:source           "star market"
                             :unit-volume      1
                             :unit-volume-type ::vol/qt
                             :unit-mass        968
                             :unit-mass-type   ::mass/g
                             :unit-cost        7.5}
                        #::g{:source           "star market"
                             :unit-volume      1
                             :unit-volume-type ::vol/pt
                             :unit-mass        484
                             :unit-mass-type   ::mass/g
                             :unit-cost        5.5}]
                       :type ::g/heavy-cream}
                  #::g{:units [] :type ::g/salt}))

(def recipe-no-id {::r/name         "foobar"
                   ::r/type         ::r/ice-cream
                   ::r/instructions ["mix it all together"]
                   ::r/amount       1
                   ::r/amount-unit  ::vol/qt
                   ::r/ingredients  [{::r/ingredient-type ::g/milk
                                      ::r/amount          1
                                      ::r/amount-unit     ::vol/c}
                                     {::r/ingredient-type ::g/heavy-cream
                                      ::r/amount          2
                                      ::r/amount-unit     ::vol/c}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id ::r/ingredients [{::r/ingredient-type ::g/milk
                                        ::r/amount          1
                                        ::r/amount-unit     ::vol/pt}
                                       {::r/ingredient-type ::g/heavy-cream
                                        ::r/amount          2
                                        ::r/amount-unit     ::vol/pt}
                                       {::r/ingredient-type ::g/salt
                                        ::r/amount          1
                                        ::r/amount-unit     ::u/pinch}]))

(def recipe-with-id
  (assoc recipe-no-id ::r/id (java.util.UUID/randomUUID)
                      ::r/name "fizzbuz"))

(def another-recipe-with-id
  (assoc recipe-no-id ::r/id (java.util.UUID/randomUUID)
                      ::r/name "another"))

(def recipe-mixins [{::r/id           #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                     ::r/name         "crushed oreo"
                     ::r/type         ::r/mixin
                     ::r/instructions ["crush 'em"]
                     ::r/amount       0.5
                     ::r/amount-unit  ::vol/c
                     ::r/ingredients  [{::r/ingredient-type ::g/oreo
                                        ::r/amount          12
                                        ::r/amount-unit     ::u/unit}]}
                    {::r/id           #uuid"dc5edc8e-79cf-4601-aab6-778d4897106a"
                     ::r/name         "oreo milk"
                     ::r/type         ::r/mixin
                     ::r/instructions ["more"]
                     ::r/amount       1
                     ::r/amount-unit  ::vol/c
                     ::r/ingredients  [{::r/ingredient-type ::g/milk
                                        ::r/amount          1
                                        ::r/amount-unit     ::vol/c}]
                     ::r/mixins       [{::r/id          #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                                        ::r/amount      0.25
                                        ::r/amount-unit ::vol/c}]}])

(def recipe-no-id-with-mixins
  (assoc recipe-no-id ::r/mixins (map #(hash-map ::r/id (::r/id %)
                                                 ::r/amount (* 0.5 (::r/amount %))
                                                 ::r/amount-unit (::r/amount-unit %)) recipe-mixins)))

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
      [recipe-with-id] (assoc recipe-with-id ::r/name "duplicate ID") [(assoc recipe-with-id ::r/name "duplicate ID")]
      ;; add invalid does not add
      [recipe-with-id] (dissoc another-recipe-with-id ::r/name) [recipe-with-id])))

(deftest add-recipe-no-id-test
  (testing "A recipe with no ID is assigned one"
    (is (let [added-recipe (first (r/add-recipe nil recipe-no-id))]
          (and (not (contains? recipe-no-id ::r/id))
               (contains? added-recipe ::r/id)
               (= (dissoc added-recipe ::r/id) recipe-no-id))))))

(deftest materialize-mixins-test
  (testing "Mixins can recursively be materialized"
    (is (= (r/materialize-mixins recipe-no-id-with-mixins recipe-mixins)
           '(#::r{:amount       0.25
                  :amount-unit  ::vol/c
                  :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                  :ingredients  (#::r{:amount          1.5
                                      :amount-unit     ::u/unit
                                      :ingredient-type ::g/oreo})
                  :instructions ["crush 'em"]
                  :mixins       ()
                  :name         "crushed oreo"
                  :type         ::r/mixin}
              #::r{:amount       0.5
                   :amount-unit  ::vol/c
                   :id           #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"
                   :ingredients  (#::r{:amount          0.5
                                       :amount-unit     ::vol/c
                                       :ingredient-type ::g/milk})
                   :instructions ["more"]
                   :mixins       (#::r{:amount      0.125
                                       :amount-unit ::vol/c
                                       :id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"})
                   :name         "oreo milk"
                   :type         ::r/mixin}
              #::r{:amount       0.125
                   :amount-unit  ::vol/c
                   :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                   :ingredients  (#::r{:amount          0.75
                                       :amount-unit     ::u/unit
                                       :ingredient-type ::g/oreo})
                   :instructions ["crush 'em"]
                   :mixins       ()
                   :name         "crushed oreo"
                   :type         ::r/mixin})))))

(deftest scale-recipe-test
  (testing "A recipe can be scaled up and down"
    (are [recipe amount amount-unit expected]
      (= (r/scale-recipe recipe amount amount-unit) expected)
      recipe-no-id 3 ::vol/qt {::r/name         "foobar"
                               ::r/type         ::r/ice-cream
                               ::r/instructions ["mix it all together"]
                               ::r/amount       3
                               ::r/amount-unit  ::vol/qt
                               ::r/ingredients  [{::r/ingredient-type ::g/milk
                                                  ::r/amount          3.0
                                                  ::r/amount-unit     ::vol/c}
                                                 {::r/ingredient-type ::g/heavy-cream
                                                  ::r/amount          6.0
                                                  ::r/amount-unit     ::vol/c}]
                               ::r/mixins       []}
      recipe-no-id 0.5 ::vol/qt {::r/name         "foobar"
                                 ::r/type         ::r/ice-cream
                                 ::r/instructions ["mix it all together"]
                                 ::r/amount       0.5
                                 ::r/amount-unit  ::vol/qt
                                 ::r/ingredients  [{::r/ingredient-type ::g/milk
                                                    ::r/amount          0.5
                                                    ::r/amount-unit     ::vol/c}
                                                   {::r/ingredient-type ::g/heavy-cream
                                                    ::r/amount          1.0
                                                    ::r/amount-unit     ::vol/c}]
                                 ::r/mixins       []}
      recipe-no-id 4 ::vol/c {::r/name         "foobar"
                              ::r/type         ::r/ice-cream
                              ::r/instructions ["mix it all together"]
                              ::r/amount       4
                              ::r/amount-unit  ::vol/c
                              ::r/ingredients  [{::r/ingredient-type ::g/milk
                                                 ::r/amount          1.0
                                                 ::r/amount-unit     ::vol/c}
                                                {::r/ingredient-type ::g/heavy-cream
                                                 ::r/amount          2.0
                                                 ::r/amount-unit     ::vol/c}]
                              ::r/mixins       []}
      recipe-no-id 2 ::vol/l {::r/name         "foobar"
                              ::r/type         ::r/ice-cream
                              ::r/instructions ["mix it all together"]
                              ::r/amount       2
                              ::r/amount-unit  ::vol/l
                              ::r/ingredients  [{::r/ingredient-type ::g/milk
                                                 ::r/amount          (* 1 2 (u/convert 1 ::vol/l ::vol/qt))
                                                 ::r/amount-unit     ::vol/c}
                                                {::r/ingredient-type ::g/heavy-cream
                                                 ::r/amount          (* 2 2 (u/convert 1 ::vol/l ::vol/qt))
                                                 ::r/amount-unit     ::vol/c}]
                              ::r/mixins       []}
      recipe-no-id-with-mixins 3 ::vol/qt {::r/name         "foobar"
                                           ::r/type         ::r/ice-cream
                                           ::r/instructions ["mix it all together"]
                                           ::r/amount       3
                                           ::r/amount-unit  ::vol/qt
                                           ::r/ingredients  [{::r/ingredient-type ::g/milk
                                                              ::r/amount          3.0
                                                              ::r/amount-unit     ::vol/c}
                                                             {::r/ingredient-type ::g/heavy-cream
                                                              ::r/amount          6.0
                                                              ::r/amount-unit     ::vol/c}]
                                           ::r/mixins       [{::r/amount      0.75
                                                              ::r/amount-unit ::vol/c
                                                              ::r/id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
                                                             {::r/amount      1.5
                                                              ::r/amount-unit ::vol/c
                                                              ::r/id          #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"}]})))

(deftest merge-recipe-ingredients-test
  (testing "a list of ingredients can be made from combining several recipes"
    (are [recipes expected]
      (= (r/merge-recipe-ingredients recipes recipes) expected)
      [recipe-no-id (r/scale-recipe recipe-no-id 2 ::vol/qt)] [{::r/ingredient-type ::g/milk
                                                                ::r/amount          3.0
                                                                ::r/amount-unit     ::vol/c}
                                                               {::r/ingredient-type ::g/heavy-cream
                                                                ::r/amount          6.0
                                                                ::r/amount-unit     ::vol/c}]
      [recipe-no-id recipe-no-id-different-ingredients] [{::r/ingredient-type ::g/milk
                                                          ::r/amount          3.0
                                                          ::r/amount-unit     ::vol/c}
                                                         {::r/ingredient-type ::g/heavy-cream
                                                          ::r/amount          6.0
                                                          ::r/amount-unit     ::vol/c}
                                                         {::r/ingredient-type ::g/salt
                                                          ::r/amount          1
                                                          ::r/amount-unit     ::u/pinch}])))

(deftest to-grocery-purchase-list-test
  (testing "recipe ingredients can be turned into grocery lists"
    (is (= (r/to-grocery-purchase-list
             (r/merge-recipe-ingredients
               [(r/scale-recipe recipe-no-id-different-ingredients 1 ::vol/gal)
                recipe-no-id]
               [recipe-no-id])
             groceries)
           {:purchase-list '(#::g{:type                  ::g/milk
                                  :amount-needed         4.5
                                  ::g/amount-needed-unit ::vol/pt
                                  :units                 (#::g{:source                 "star market"
                                                               :unit-cost              5.0
                                                               :unit-mass              3.9
                                                               :unit-mass-type         ::mass/kg
                                                               :unit-purchase-quantity 1
                                                               :unit-volume            1
                                                               :unit-volume-type       ::vol/gal})}
                              #::g{:type                  ::g/heavy-cream
                                   :amount-needed         9.0
                                   ::g/amount-needed-unit ::vol/pt
                                   :units                 (#::g{:source                 "star market"
                                                                :unit-cost              7.5
                                                                :unit-mass              968
                                                                :unit-mass-type         ::mass/g
                                                                :unit-purchase-quantity 4
                                                                :unit-volume            1
                                                                :unit-volume-type       ::vol/qt}
                                                            #::g{:source                 "star market"
                                                                 :unit-cost              5.5
                                                                 :unit-mass              484
                                                                 :unit-mass-type         ::mass/g
                                                                 :unit-purchase-quantity 1
                                                                 :unit-volume            1
                                                                 :unit-volume-type       ::vol/pt})}
                              #::g{:type                  ::g/salt
                                   :amount-needed         4.0
                                   ::g/amount-needed-unit ::u/pinch
                                   :units                 nil})
            :total-cost    40.5}))))
