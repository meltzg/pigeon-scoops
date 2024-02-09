(ns pigeon-scoops.components.grocery-manager-test
  (:require [clojure.test :as t]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as u]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]))

(def grocery-item
  #::gs{:type        ::gs/milk
        :description "moo moo juice"
        :units       [#::gs{:source           "dark market"
                            :unit-cost        6.5
                            :unit-volume      1.0
                            :unit-volume-type ::volume/gal}
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
        :unit-volume-type ::volume/gal
        :unit-mass        1.94
        :unit-mass-type   ::mass/kg
        :unit-cost        7})

(def quart
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::volume/qt
        :unit-mass        968
        :unit-mass-type   ::mass/g
        :unit-cost        4.5})

(def pint
  #::gs{:source           "star market"
        :unit-volume      1
        :unit-volume-type ::volume/pt
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

(t/deftest get-grocery-unit-for-amount
  (t/testing "smallest possible grocery unit is returned"
    (t/are [amount amount-unit item expected]
      (= (gm/get-grocery-unit-for-amount amount amount-unit item) expected)
      1 ::u/unit common-unit-grocery-item eggs-12
      12 ::u/unit common-unit-grocery-item eggs-12
      13 ::u/unit common-unit-grocery-item eggs-18
      20 ::u/unit common-unit-grocery-item eggs-18
      36 ::u/unit common-unit-grocery-item eggs-18
      1 ::volume/c mass-volume-unit-grocery-item pint
      4 ::mass/kg mass-volume-unit-grocery-item half-gal
      4 ::u/pinch no-units-grocery-item nil
      4 ::volume/qt mass-only-unit-grocery-item nil)))

(t/deftest divide-grocery-test
  (t/testing "an amount can be divided into a set of unit amounts"
    (t/are [amount amount-unit item expected-units expected-purchase-amount expected-purchase-unit needed-cost purchase-cost]
      (= (gm/divide-grocery amount amount-unit item) (assoc item ::gs/units expected-units
                                                                 ::gs/amount-needed amount
                                                                 ::gs/amount-needed-unit amount-unit
                                                                 ::gs/amount-needed-cost needed-cost
                                                                 ::gs/purchase-amount expected-purchase-amount
                                                                 ::gs/purchase-amount-unit expected-purchase-unit
                                                                 ::gs/purchase-cost purchase-cost))
      0 ::u/unit common-unit-grocery-item nil nil nil nil nil
      -1 ::u/unit common-unit-grocery-item nil nil nil nil nil
      4 ::u/pinch no-units-grocery-item nil nil nil nil nil
      1 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)] 12 ::u/unit 0.41583333333333333 4.99
      12 ::u/unit common-unit-grocery-item [(assoc eggs-12 ::gs/unit-purchase-quantity 1)] 12 ::u/unit 4.99 4.99
      13 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)] 18 ::u/unit 5.337222222222222 7.39
      20 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 1)
                                            (assoc eggs-12 ::gs/unit-purchase-quantity 1)] 30 ::u/unit 8.253333333333334 12.379999999999999
      36 ::u/unit common-unit-grocery-item [(assoc eggs-18 ::gs/unit-purchase-quantity 2)] 36 ::u/unit 14.78 14.78
      1 ::volume/c mass-volume-unit-grocery-item [(assoc pint ::gs/unit-purchase-quantity 1)] 1 ::volume/pt 1.4 2.8
      4 ::mass/kg mass-volume-unit-grocery-item [(assoc half-gal ::gs/unit-purchase-quantity 2)
                                                 (assoc pint ::gs/unit-purchase-quantity 1)] 4.364 ::mass/kg 15.398716773602201 16.8)))
