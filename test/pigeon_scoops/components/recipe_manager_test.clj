(ns pigeon-scoops.components.recipe-manager-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def groceries '(#::gm{:units
                       [#::gm{:source           "star market"
                              :unit-volume      1
                              :unit-volume-type ::vol/gal
                              :unit-mass        3.9
                              :unit-mass-type   ::mass/kg
                              :unit-cost        5.0}
                        #::gm{:source           "star market"
                              :unit-volume      0.5
                              :unit-volume-type ::vol/gal
                              :unit-mass        1.95
                              :unit-mass-type   ::mass/kg
                              :unit-cost        3.5}]
                       :type ::gm/milk}
                  #::gm{:units
                        [#::gm{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::vol/qt
                               :unit-mass        968
                               :unit-mass-type   ::mass/g
                               :unit-cost        7.5}
                         #::gm{:source           "star market"
                               :unit-volume      1
                               :unit-volume-type ::vol/pt
                               :unit-mass        484
                               :unit-mass-type   ::mass/g
                               :unit-cost        5.5}]
                        :type ::gm/heavy-cream}
                  #::gm{:units [] :type ::gm/salt}))

(def recipe-no-id {::rm/name         "foobar"
                   ::rm/type         ::rm/ice-cream
                   ::rm/instructions ["mix it all together"]
                   ::rm/amount       1
                   ::rm/amount-unit  ::vol/qt
                   ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                       ::rm/amount          1
                                       ::rm/amount-unit     ::vol/c}
                                      {::rm/ingredient-type ::gm/heavy-cream
                                       ::rm/amount          2
                                       ::rm/amount-unit     ::vol/c}]})

(def recipe-no-id-different-ingredients
  (assoc recipe-no-id ::rm/ingredients [{::rm/ingredient-type ::gm/milk
                                         ::rm/amount          1
                                         ::rm/amount-unit     ::vol/pt}
                                        {::rm/ingredient-type ::gm/heavy-cream
                                         ::rm/amount          2
                                         ::rm/amount-unit     ::vol/pt}
                                        {::rm/ingredient-type ::gm/salt
                                         ::rm/amount          1
                                         ::rm/amount-unit     ::u/pinch}]))

(def recipe-with-id
  (assoc recipe-no-id ::rm/id (java.util.UUID/randomUUID)
                      ::rm/name "fizzbuz"))

(def another-recipe-with-id
  (assoc recipe-no-id ::rm/id (java.util.UUID/randomUUID)
                      ::rm/name "another"))

(def recipe-mixins [{::rm/id           #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                     ::rm/name         "crushed oreo"
                     ::rm/type         ::rm/mixin
                     ::rm/instructions ["crush 'em"]
                     ::rm/amount       0.5
                     ::rm/amount-unit  ::vol/c
                     ::rm/ingredients  [{::rm/ingredient-type ::gm/oreo
                                         ::rm/amount          12
                                         ::rm/amount-unit     ::u/unit}]}
                    {::rm/id           #uuid"dc5edc8e-79cf-4601-aab6-778d4897106a"
                     ::rm/name         "oreo milk"
                     ::rm/type         ::rm/mixin
                     ::rm/instructions ["more"]
                     ::rm/amount       1
                     ::rm/amount-unit  ::vol/c
                     ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                         ::rm/amount          1
                                         ::rm/amount-unit     ::vol/c}]
                     ::rm/mixins       [{::rm/id          #uuid"c7044068-329e-4323-b814-d65bf3da6ba3"
                                         ::rm/amount      0.25
                                         ::rm/amount-unit ::vol/c}]}])

(def recipe-no-id-with-mixins
  (assoc recipe-no-id ::rm/mixins (map #(hash-map ::rm/id (::rm/id %)
                                                  ::rm/amount (* 0.5 (::rm/amount %))
                                                  ::rm/amount-unit (::rm/amount-unit %)) recipe-mixins)))

(deftest get-recipes-test
  (testing "Recipes can be retrieved by ID"
    (are [ids expected]
      (= (set (apply (partial rm/get-recipes {::rm/recipes (atom [recipe-with-id another-recipe-with-id])}) ids)) (set expected))
      [] [recipe-with-id another-recipe-with-id]
      [(::rm/id recipe-with-id)] [recipe-with-id]
      [(::rm/id recipe-with-id) (::rm/id another-recipe-with-id)] [recipe-with-id another-recipe-with-id]
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
      [recipe-with-id] (assoc recipe-with-id ::rm/name "duplicate ID") [(assoc recipe-with-id ::rm/name "duplicate ID")]
      ;; add invalid does not add
      [recipe-with-id] (dissoc another-recipe-with-id ::rm/name) [recipe-with-id])))

(deftest add-recipe-no-id-test
  (testing "A recipe with no ID is assigned one"
    (is (let [added-recipe (first (rm/add-recipe nil recipe-no-id))]
          (and (not (contains? recipe-no-id ::rm/id))
               (contains? added-recipe ::rm/id)
               (= (dissoc added-recipe ::rm/id) recipe-no-id))))))

(deftest materialize-mixins-test
  (testing "Mixins can recursively be materialized"
    (is (= (rm/materialize-mixins recipe-no-id-with-mixins recipe-mixins)
           '(#::rm{:amount       0.25
                   :amount-unit  ::vol/c
                   :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                   :ingredients  (#::rm{:amount          1.5
                                        :amount-unit     ::u/unit
                                        :ingredient-type ::gm/oreo})
                   :instructions ["crush 'em"]
                   :mixins       ()
                   :name         "crushed oreo"
                   :type         ::rm/mixin}
              #::rm{:amount       0.5
                    :amount-unit  ::vol/c
                    :id           #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"
                    :ingredients  (#::rm{:amount          0.5
                                         :amount-unit     ::vol/c
                                         :ingredient-type ::gm/milk})
                    :instructions ["more"]
                    :mixins       (#::rm{:amount      0.125
                                         :amount-unit ::vol/c
                                         :id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"})
                    :name         "oreo milk"
                    :type         ::rm/mixin}
              #::rm{:amount       0.125
                    :amount-unit  ::vol/c
                    :id           #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"
                    :ingredients  (#::rm{:amount          0.75
                                         :amount-unit     ::u/unit
                                         :ingredient-type ::gm/oreo})
                    :instructions ["crush 'em"]
                    :mixins       ()
                    :name         "crushed oreo"
                    :type         ::rm/mixin})))))

(deftest scale-recipe-test
  (testing "A recipe can be scaled up and down"
    (are [recipe amount amount-unit expected]
      (= (rm/scale-recipe recipe amount amount-unit) expected)
      recipe-no-id 3 ::vol/qt {::rm/name         "foobar"
                               ::rm/type         ::rm/ice-cream
                               ::rm/instructions ["mix it all together"]
                               ::rm/amount       3
                               ::rm/amount-unit  ::vol/qt
                               ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                                   ::rm/amount          3.0
                                                   ::rm/amount-unit     ::vol/c}
                                                  {::rm/ingredient-type ::gm/heavy-cream
                                                   ::rm/amount          6.0
                                                   ::rm/amount-unit     ::vol/c}]
                               ::rm/mixins       []}
      recipe-no-id 0.5 ::vol/qt {::rm/name         "foobar"
                                 ::rm/type         ::rm/ice-cream
                                 ::rm/instructions ["mix it all together"]
                                 ::rm/amount       0.5
                                 ::rm/amount-unit  ::vol/qt
                                 ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                                     ::rm/amount          0.5
                                                     ::rm/amount-unit     ::vol/c}
                                                    {::rm/ingredient-type ::gm/heavy-cream
                                                     ::rm/amount          1.0
                                                     ::rm/amount-unit     ::vol/c}]
                                 ::rm/mixins       []}
      recipe-no-id 4 ::vol/c {::rm/name         "foobar"
                              ::rm/type         ::rm/ice-cream
                              ::rm/instructions ["mix it all together"]
                              ::rm/amount       4
                              ::rm/amount-unit  ::vol/c
                              ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                                  ::rm/amount          1.0
                                                  ::rm/amount-unit     ::vol/c}
                                                 {::rm/ingredient-type ::gm/heavy-cream
                                                  ::rm/amount          2.0
                                                  ::rm/amount-unit     ::vol/c}]
                              ::rm/mixins       []}
      recipe-no-id 2 ::vol/l {::rm/name         "foobar"
                              ::rm/type         ::rm/ice-cream
                              ::rm/instructions ["mix it all together"]
                              ::rm/amount       2
                              ::rm/amount-unit  ::vol/l
                              ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                                  ::rm/amount          (* 1 2 (u/convert 1 ::vol/l ::vol/qt))
                                                  ::rm/amount-unit     ::vol/c}
                                                 {::rm/ingredient-type ::gm/heavy-cream
                                                  ::rm/amount          (* 2 2 (u/convert 1 ::vol/l ::vol/qt))
                                                  ::rm/amount-unit     ::vol/c}]
                              ::rm/mixins       []}
      recipe-no-id-with-mixins 3 ::vol/qt {::rm/name         "foobar"
                                           ::rm/type         ::rm/ice-cream
                                           ::rm/instructions ["mix it all together"]
                                           ::rm/amount       3
                                           ::rm/amount-unit  ::vol/qt
                                           ::rm/ingredients  [{::rm/ingredient-type ::gm/milk
                                                               ::rm/amount          3.0
                                                               ::rm/amount-unit     ::vol/c}
                                                              {::rm/ingredient-type ::gm/heavy-cream
                                                               ::rm/amount          6.0
                                                               ::rm/amount-unit     ::vol/c}]
                                           ::rm/mixins       [{::rm/amount      0.75
                                                               ::rm/amount-unit ::vol/c
                                                               ::rm/id          #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
                                                              {::rm/amount      1.5
                                                               ::rm/amount-unit ::vol/c
                                                               ::rm/id          #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"}]})))

(deftest merge-recipe-ingredients-test
  (testing "a list of ingredients can be made from combining several recipes"
    (are [recipes expected]
      (= (rm/merge-recipe-ingredients recipes (concat recipe-mixins recipes)) expected)
      [recipe-no-id (rm/scale-recipe recipe-no-id 2 ::vol/qt)] [{::rm/ingredient-type ::gm/milk
                                                                 ::rm/amount          3.0
                                                                 ::rm/amount-unit     ::vol/c}
                                                                {::rm/ingredient-type ::gm/heavy-cream
                                                                 ::rm/amount          6.0
                                                                 ::rm/amount-unit     ::vol/c}]
      [recipe-no-id recipe-no-id-different-ingredients] [{::rm/ingredient-type ::gm/milk
                                                          ::rm/amount          3.0
                                                          ::rm/amount-unit     ::vol/c}
                                                         {::rm/ingredient-type ::gm/heavy-cream
                                                          ::rm/amount          6.0
                                                          ::rm/amount-unit     ::vol/c}
                                                         {::rm/ingredient-type ::gm/salt
                                                          ::rm/amount          1
                                                          ::rm/amount-unit     ::u/pinch}]
      [recipe-no-id-with-mixins (rm/scale-recipe recipe-no-id-with-mixins 2 ::vol/qt)] [{::rm/ingredient-type ::gm/oreo
                                                                                         ::rm/amount          6.75
                                                                                         ::rm/amount-unit     ::u/unit}
                                                                                        {::rm/ingredient-type ::gm/milk
                                                                                         ::rm/amount          4.5
                                                                                         ::rm/amount-unit     ::vol/c}
                                                                                        {::rm/ingredient-type ::gm/heavy-cream
                                                                                         ::rm/amount          6.0
                                                                                         ::rm/amount-unit     ::vol/c}])))

(deftest to-grocery-purchase-list-test
  (testing "recipe ingredients can be turned into grocery lists"
    (is (= (rm/to-grocery-purchase-list
             (rm/merge-recipe-ingredients
               [(rm/scale-recipe recipe-no-id-different-ingredients 1 ::vol/gal)
                recipe-no-id]
               [recipe-no-id])
             groceries)
           {:purchase-list '(#::gm{:type                   ::gm/milk
                                   :amount-needed          4.5
                                   ::gm/amount-needed-unit ::vol/pt
                                   :units                  (#::gm{:source                 "star market"
                                                                  :unit-cost              5.0
                                                                  :unit-mass              3.9
                                                                  :unit-mass-type         ::mass/kg
                                                                  :unit-purchase-quantity 1
                                                                  :unit-volume            1
                                                                  :unit-volume-type       ::vol/gal})}
                              #::gm{:type                   ::gm/heavy-cream
                                    :amount-needed          9.0
                                    ::gm/amount-needed-unit ::vol/pt
                                    :units                  (#::gm{:source                 "star market"
                                                                   :unit-cost              7.5
                                                                   :unit-mass              968
                                                                   :unit-mass-type         ::mass/g
                                                                   :unit-purchase-quantity 4
                                                                   :unit-volume            1
                                                                   :unit-volume-type       ::vol/qt}
                                                              #::gm{:source                 "star market"
                                                                    :unit-cost              5.5
                                                                    :unit-mass              484
                                                                    :unit-mass-type         ::mass/g
                                                                    :unit-purchase-quantity 1
                                                                    :unit-volume            1
                                                                    :unit-volume-type       ::vol/pt})}
                              #::gm{:type                   ::gm/salt
                                    :amount-needed          4.0
                                    ::gm/amount-needed-unit ::u/pinch
                                    :units                  nil})
            :total-cost    40.5}))))
