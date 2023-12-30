(ns pigeon-scoops.components.grocery-manager-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.components.grocery-manager :as g]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def grocery-item
  #::g{:type        ::g/milk
       :description "moo moo juice"
       :units       [#::g{:source           "dark market"
                          :unit-cost        6.5
                          :unit-volume      1.0
                          :unit-volume-type ::vol/gal}
                     #::g{:source         "dark market"
                          :unit-cost      3.25
                          :unit-mass      1.95
                          :unit-mass-type ::mass/kg}]})

(def eggs-12
  #::g{:source           "star market"
       :unit-common      12
       :unit-common-type ::u/unit
       :unit-cost        4.99})

(def eggs-18
  #::g{:source           "star market"
       :unit-common      18
       :unit-common-type ::u/unit
       :unit-cost        7.39})

(def common-unit-grocery-item
  #::g{:type        ::g/egg-yolk
       :description "need to figure out what to do with whites"
       :units       [eggs-12 eggs-18]})

(def half-gal
  #::g{:source           "star market"
       :unit-volume      0.5
       :unit-volume-type ::vol/gal
       :unit-mass        1.94
       :unit-mass-type   ::mass/kg
       :unit-cost        7})

(def quart
  #::g{:source           "star market"
       :unit-volume      1
       :unit-volume-type ::vol/qt
       :unit-mass        968
       :unit-mass-type   ::mass/g
       :unit-cost        4.5})

(def pint
  #::g{:source           "star market"
       :unit-volume      1
       :unit-volume-type ::vol/pt
       :unit-mass        484
       :unit-mass-type   ::mass/g
       :unit-cost        2.8})

(def mass-volume-unit-grocery-item
  #::g{:units [half-gal quart pint]
       :type  ::g/half-and-half})

(def mass-only-unit-grocery-item
  #::g{:units (map #(dissoc % ::g/unit-volume ::g/unit-volume-type) [half-gal quart pint])
       :type  ::g/half-and-half})

(def no-units-grocery-item
  #::g{:units [] :type ::g/salt})

(def another-grocery-item
  (assoc grocery-item ::g/type ::g/heavy-cream
                      ::g/description "heavy moo moo juice"))


(deftest add-grocery-item-test
  (testing "Valid grocery items can be added to collection of grocery-items"
    (are [grocery-items new-grocery-item expected valid?]
      (let [actual (g/add-grocery-item {::g/groceries (atom grocery-items)} new-grocery-item)]
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
      [grocery-item] (assoc grocery-item ::g/description "duplicate type") nil true
      ;; add invalid returns error explanation
      [grocery-item] (dissoc another-grocery-item ::g/type) [grocery-item] false)))

(deftest update-grocery-item-test
  (testing "Valid grocery items can be updated"
    (are [grocery-items new-grocery-item expected valid?]
      (let [actual (g/add-grocery-item {::g/groceries (atom grocery-items)} new-grocery-item true)]
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
      [grocery-item] (assoc grocery-item ::g/description "duplicate type") [(assoc grocery-item ::g/description "duplicate type")] true
      ;; add new invalid returns nil
      [grocery-item] (dissoc another-grocery-item ::g/units) nil true
      ;; update existing with invalid returns error explanation
      [grocery-item] (-> grocery-item
                         (assoc ::g/description "duplicate type")
                         (dissoc ::g/units)) nil false)))

(deftest delete-grocery-item-test
  (testing "Grocery items can be deleted"
    (are [grocery-items type-to-delete expected]
      (= (set (g/delete-grocery-item {::g/groceries (atom grocery-items)} type-to-delete))
         (set expected))
      ;; existing item is removed
      [grocery-item another-grocery-item] (::g/type another-grocery-item) [grocery-item]
      ;; missing item removes nothing
      [grocery-item another-grocery-item] ::g/missing-type [grocery-item another-grocery-item])))

(deftest get-groceries-test
  (testing "Groceries can be retrieved by type"
    (are [types expected]
      (= (set (apply (partial g/get-groceries {::g/groceries (atom [grocery-item common-unit-grocery-item another-grocery-item])}) types)) (set expected))
      [] [grocery-item common-unit-grocery-item another-grocery-item]
      [(::g/type grocery-item)] [grocery-item]
      [(::g/type another-grocery-item) (::g/type common-unit-grocery-item)] [another-grocery-item common-unit-grocery-item]
      [::g/missing-type] [])))

(deftest get-grocery-unit-for-amount
  (testing "smallest possible grocery unit is returned"
    (are [amount amount-unit item expected]
      (= (g/get-grocery-unit-for-amount amount amount-unit item) expected)
      1 ::u/unit common-unit-grocery-item eggs-12
      12 ::u/unit common-unit-grocery-item eggs-12
      13 ::u/unit common-unit-grocery-item eggs-18
      20 ::u/unit common-unit-grocery-item eggs-18
      36 ::u/unit common-unit-grocery-item eggs-18
      1 ::vol/c mass-volume-unit-grocery-item pint
      4 ::mass/kg mass-volume-unit-grocery-item half-gal
      4 ::u/pinch no-units-grocery-item nil
      4 ::vol/qt mass-only-unit-grocery-item nil)))

(deftest divide-grocery-test
  (testing "an amount can be divided into a set of unit amounts"
    (are [amount amount-unit item expected]
      (= (g/divide-grocery amount amount-unit item) (assoc item ::g/units expected
                                                                ::g/amount-needed amount
                                                                ::g/amount-needed-unit amount-unit))
      0 ::u/unit common-unit-grocery-item nil
      -1 ::u/unit common-unit-grocery-item nil
      4 ::u/pinch no-units-grocery-item nil
      1 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::g/unit-purchase-quantity 1)]
      12 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::g/unit-purchase-quantity 1)]
      13 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::g/unit-purchase-quantity 1)]
      20 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::g/unit-purchase-quantity 1)
                                            (assoc eggs-12 ::g/unit-purchase-quantity 1)]
      36 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::g/unit-purchase-quantity 2)]
      1 ::vol/c mass-volume-unit-grocery-item [(assoc pint ::g/unit-purchase-quantity 1)]
      4 ::mass/kg mass-volume-unit-grocery-item [(assoc half-gal ::g/unit-purchase-quantity 2)
                                                 (assoc pint ::g/unit-purchase-quantity 1)])))
