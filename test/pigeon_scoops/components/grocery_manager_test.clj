(ns pigeon-scoops.components.grocery-manager-test
  (:require [clojure.test :as t]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def grocery-item
  #::gs{:type        ::gs/milk
        :description "moo moo juice"
        :units       [#::gs{:source           "dark market"
                            :unit-cost        6.5
                            :unit-volume      1.0
                            :unit-volume-type ::vol/gal}
                      #::gs{:source         "dark market"
                            :unit-cost      3.25
                            :unit-mass      1.95
                            :unit-mass-type ::mass/kg}]})

(def eggs-12
  #::gs{:source           "star market"
        :unit-common      12
        :unit-common-type ::u/unit
        :unit-cost        4.99})

(def eggs-18
  #::gs{:source           "star market"
        :unit-common      18
        :unit-common-type ::u/unit
        :unit-cost        7.39})

(def common-unit-grocery-item
  #::gs{:type        ::gs/egg-yolk
        :description "need to figure out what to do with whites"
        :units       [eggs-12 eggs-18]})

(def half-gal
  #::gs{:source           "star market"
        :unit-volume      0.5
        :unit-volume-type ::vol/gal
        :unit-mass        1.94
        :unit-mass-type   ::mass/kg
        :unit-cost        7})

(def quart
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::vol/qt
        :unit-mass        968
        :unit-mass-type   ::mass/g
        :unit-cost        4.5})

(def pint
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::vol/pt
        :unit-mass        484
        :unit-mass-type   ::mass/g
        :unit-cost        2.8})

(def mass-volume-unit-grocery-item
  #::gs{:units [half-gal quart pint]
        :type  ::gs/half-and-half})

(def mass-only-unit-grocery-item
  #::gs{:units (map #(dissoc % ::gs/unit-volume ::gs/unit-volume-type) [half-gal quart pint])
        :type  ::gs/half-and-half})

(def no-units-grocery-item
  #::gs{:units [] :type ::gs/salt})

(def another-grocery-item
  (assoc grocery-item ::gs/type ::gs/heavy-cream
                      ::gs/description "heavy moo moo juice"))


(t/deftest add-grocery-item-test
  (t/testing "Valid grocery items can be added to collection of grocery-items"
    (t/are [grocery-items new-grocery-item expected valid?]
      (let [actual (gm/add-grocery-item {::gm/groceries (atom grocery-items)} new-grocery-item)]
        (if valid?
          (= (set actual) (set expected))
          (:clojure.spec.alpha/problems actual)))
      ;; add grocery item to nil collection
      nil grocery-item (list grocery-item) true
      ;; add grocery item to empty collection
      [] grocery-item [grocery-item] true
      ;; add grocery item to existing collection
      [grocery-item] another-grocery-item [grocery-item another-grocery-item] true
      ;; add duplicate returns nil
      [grocery-item] (assoc grocery-item ::gs/description "duplicate type") nil true
      ;; add invalid returns error explanation
      [grocery-item] (dissoc another-grocery-item ::gs/type) [grocery-item] false)))

(t/deftest update-grocery-item-test
  (t/testing "Valid grocery items can be updated"
    (t/are [grocery-items new-grocery-item expected valid?]
      (let [actual (gm/add-grocery-item {::gm/groceries (atom grocery-items)} new-grocery-item true)]
        (if valid?
          (= (set actual) (set expected))
          (:clojure.spec.alpha/problems actual)))
      ;; add new grocery item to nil collection returns nil
      nil grocery-item nil true
      ;; add new grocery item to empty collection returns nil
      [] grocery-item nil true
      ;; add new grocery item returns nil
      [grocery-item] another-grocery-item nil true
      ;; add duplicate type keeps new
      [grocery-item] (assoc grocery-item ::gs/description "duplicate type") [(assoc grocery-item ::gs/description "duplicate type")] true
      ;; add new invalid returns nil
      [grocery-item] (dissoc another-grocery-item ::gs/units) nil true
      ;; update existing with invalid returns error explanation
      [grocery-item] (-> grocery-item
                         (assoc ::gs/description "duplicate type")
                         (dissoc ::gs/units)) nil false)))

(t/deftest delete-grocery-item-test
  (t/testing "Grocery items can be deleted"
    (t/are [grocery-items type-to-delete expected]
      (= (set (gm/delete-grocery-item {::gm/groceries (atom grocery-items)} type-to-delete))
         (set expected))
      ;; existing item is removed
      [grocery-item another-grocery-item] (::gs/type another-grocery-item) [grocery-item]
      ;; missing item removes nothing
      [grocery-item another-grocery-item] ::gs/missing-type [grocery-item another-grocery-item])))

(t/deftest get-groceries-test
  (t/testing "Groceries can be retrieved by type"
    (t/are [types expected]
      (= (set (apply (partial gm/get-groceries {::gm/groceries (atom [grocery-item common-unit-grocery-item another-grocery-item])}) types)) (set expected))
      [] [grocery-item common-unit-grocery-item another-grocery-item]
      [(::gs/type grocery-item)] [grocery-item]
      [(::gs/type another-grocery-item) (::gs/type common-unit-grocery-item)] [another-grocery-item common-unit-grocery-item]
      [::gs/missing-type] [])))

(t/deftest get-grocery-unit-for-amount
  (t/testing "smallest possible grocery unit is returned"
    (t/are [amount amount-unit item expected]
      (= (gm/get-grocery-unit-for-amount amount amount-unit item) expected)
      1 ::u/unit common-unit-grocery-item eggs-12
      12 ::u/unit common-unit-grocery-item eggs-12
      13 ::u/unit common-unit-grocery-item eggs-18
      20 ::u/unit common-unit-grocery-item eggs-18
      36 ::u/unit common-unit-grocery-item eggs-18
      1 ::vol/c mass-volume-unit-grocery-item pint
      4 ::mass/kg mass-volume-unit-grocery-item half-gal
      4 ::u/pinch no-units-grocery-item nil
      4 ::vol/qt mass-only-unit-grocery-item nil)))

(t/deftest divide-grocery-test
  (t/testing "an amount can be divided into a set of unit amounts"
    (t/are [amount amount-unit item expected]
      (= (gm/divide-grocery amount amount-unit item) (assoc item ::gs/units expected
                                                                 ::gs/amount-needed amount
                                                                 ::gs/amount-needed-unit amount-unit))
      0 ::u/unit common-unit-grocery-item nil
      -1 ::u/unit common-unit-grocery-item nil
      4 ::u/pinch no-units-grocery-item nil
      1 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      12 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      13 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)]
      20 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)
                                            (assoc eggs-12 ::gs/unit-purchase-quantity 1)]
      36 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 2)]
      1 ::vol/c mass-volume-unit-grocery-item [(assoc pint ::gs/unit-purchase-quantity 1)]
      4 ::mass/kg mass-volume-unit-grocery-item [(assoc half-gal ::gs/unit-purchase-quantity 2)
                                                 (assoc pint ::gs/unit-purchase-quantity 1)])))
