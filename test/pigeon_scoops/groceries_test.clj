(ns pigeon-scoops.groceries-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.groceries :as g]))

(def grocery-item
  #:grocery{:type        :grocery/milk
            :description "moo moo juice"
            :units       [#:grocery{:source           "dark market"
                                    :unit-cost        6.5
                                    :unit-volume      1.0
                                    :unit-volume-type :volume/gal}
                          #:grocery{:source         "dark market"
                                    :unit-cost      3.25
                                    :unit-mass      1.95
                                    :unit-mass-type :mass/kg}]})

(def eggs-12
  #:grocery{:source           "star market"
            :unit-common      12
            :unit-common-type :common/unit
            :unit-cost        4.99})

(def eggs-18
  #:grocery{:source           "star market"
            :unit-common      18
            :unit-common-type :common/unit
            :unit-cost        7.39})

(def common-unit-grocery-item
  #:grocery{:type        :grocery/egg-yolk
            :description "need to figure out what to do with whites"
            :units       [eggs-12 eggs-18]})

(def half-gal
  #:grocery{:source           "star market"
            :unit-volume      0.5
            :unit-volume-type :volume/gal
            :unit-mass        1.94
            :unit-mass-type   :mass/kg
            :unit-cost        7})

(def quart
  #:grocery{:source           "star market"
            :unit-volume      1
            :unit-volume-type :volume/qt
            :unit-mass        968
            :unit-mass-type   :mass/g
            :unit-cost        4.5})

(def pint
  #:grocery{:source           "star market"
            :unit-volume      1
            :unit-volume-type :volume/pt
            :unit-mass        484
            :unit-mass-type   :mass/g
            :unit-cost        2.8})

(def mass-volume-unit-grocery-item
  #:grocery{:units [half-gal quart pint]
            :type  :grocery/half-and-half})

(def another-grocery-item
  (assoc grocery-item :grocery/type :grocery/heavy-cream
                      :grocery/description "heavy moo moo juice"))


(deftest add-ingredient-test
  (testing "Valid ingredients can be added to collection of ingredients"
    (are [ingredients new-ingredient expected]
      (= (set (g/add-grocery-item ingredients new-ingredient)) (set expected))
      ;; add grocery item to nil collection
      nil grocery-item (list grocery-item)
      ;; add grocery item to empty collection
      [] grocery-item [grocery-item]
      ;; add grocery item to existing collection
      [grocery-item] another-grocery-item [grocery-item another-grocery-item]
      ;; add duplicate type keeps new
      [grocery-item] (assoc grocery-item :grocery/description "duplicate type") [(assoc grocery-item :grocery/description "duplicate type")]
      ;; add invalid does not add
      [grocery-item] (dissoc another-grocery-item :grocery/type) [grocery-item])))

(deftest get-grocery-unit-for-amount
  (testing "smallest possible grocery unit is returned"
    (are [amount amount-unit item expected]
      (= (g/get-grocery-unit-for-amount amount amount-unit item) expected)
      1 :common/unit common-unit-grocery-item eggs-12
      12 :common/unit common-unit-grocery-item eggs-12
      13 :common/unit common-unit-grocery-item eggs-18
      20 :common/unit common-unit-grocery-item eggs-18
      36 :common/unit common-unit-grocery-item eggs-18
      1 :volume/c mass-volume-unit-grocery-item pint
      4 :mass/kg mass-volume-unit-grocery-item half-gal)))

(deftest divide-grocery-test
  (testing "an amount can be divided into a set of unit amounts"
    (are [amount amount-unit item expected]
      (= (g/divide-grocery amount amount-unit item) (assoc item :grocery/units expected))
      0 :common/unit common-unit-grocery-item nil
      -1 :common/unit common-unit-grocery-item nil
      1 :common/unit common-unit-grocery-item [(assoc eggs-12 :grocery/unit-purchase-quantity 1)]
      12 :common/unit common-unit-grocery-item [(assoc eggs-12 :grocery/unit-purchase-quantity 1)]
      13 :common/unit common-unit-grocery-item [(assoc eggs-18 :grocery/unit-purchase-quantity 1)]
      20 :common/unit common-unit-grocery-item [(assoc eggs-18 :grocery/unit-purchase-quantity 1)
                                                (assoc eggs-12 :grocery/unit-purchase-quantity 1)]
      36 :common/unit common-unit-grocery-item [(assoc eggs-18 :grocery/unit-purchase-quantity 2)]
      1 :volume/c mass-volume-unit-grocery-item [(assoc pint :grocery/unit-purchase-quantity 1)]
      4 :mass/kg mass-volume-unit-grocery-item [(assoc half-gal :grocery/unit-purchase-quantity 2)
                                                (assoc pint :grocery/unit-purchase-quantity 1)])))
