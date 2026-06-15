(ns pigeon-scoops.components.bom-table-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.components.bom-table :as bom]))

(deftest format-amount-test
  (testing "joins amount and unit name"
    (is (= "2cup" (bom/format-amount 2 :unit/cup))))
  (testing "returns nil when amount is missing"
    (is (nil? (bom/format-amount nil :unit/cup)))))

(deftest format-dollar-test
  (testing "formats with a leading dollar sign and two decimals"
    (is (= "$1.50" (bom/format-dollar 1.5)))
    (is (= "$0.00" (bom/format-dollar 0)))))

(deftest format-percentage-test
  (testing "converts a decimal ratio into a percentage string"
    (is (= "25.00%" (bom/format-percentage 0.25)))
    (is (= "0.00%" (bom/format-percentage 0)))))

(def units
  [{:grocery-unit/unit-cost 2.0 :grocery-unit/quantity 3}
   {:grocery-unit/unit-cost 1.5 :grocery-unit/quantity 2}])

(deftest purchase-cost-test
  (testing "sums unit-cost * quantity across all units"
    (is (= 9.0 (bom/purchase-cost units))))
  (testing "is zero for an empty collection"
    (is (= 0 (bom/purchase-cost [])))))

(deftest required-cost-test
  (testing "subtracts the wasted portion of the purchase cost"
    (is (= 7.2 (bom/required-cost units 0.2))))
  (testing "equals purchase cost when there is no waste"
    (is (= 9.0 (bom/required-cost units 0)))))

(deftest format-unit-test
  (testing "joins quantity with the populated mass/volume/common dimensions"
    (is (= "3 x 2lb"
           (bom/format-unit {:grocery-unit/quantity 3
                             :grocery-unit/unit-mass 2
                             :grocery-unit/unit-mass-type :unit/lb}))))
  (testing "joins multiple populated dimensions with a colon"
    (is (= "1 x 2lb:3cup"
           (bom/format-unit {:grocery-unit/quantity 1
                             :grocery-unit/unit-mass 2
                             :grocery-unit/unit-mass-type :unit/lb
                             :grocery-unit/unit-volume 3
                             :grocery-unit/unit-volume-type :unit/cup}))))
  (testing "ignores dimensions whose amount is absent"
    (is (= "1 x "
           (bom/format-unit {:grocery-unit/quantity 1})))))

(deftest add-derived-fields-test
  (testing "augments a grocery with formatted/derived bill-of-materials fields"
    (let [grocery {:grocery/required-amount 4
                   :grocery/required-unit :unit/cup
                   :grocery/purchase-amount 2
                   :grocery/purchase-unit :unit/lb
                   :grocery/waste-ratio 0.1
                   :grocery/units units}
          result (bom/add-derived-fields grocery)]
      (is (= "4cup" (:grocery/amount-needed result)))
      (is (= "2lb" (:grocery/purchase-amount result)))
      (is (= 9.0 (:grocery/purchase-cost result)))
      (is (= 8.1 (:grocery/amount-cost result)))
      (is (= 0.1 (:grocery/waste-ratio result)))
      (is (= "3 x  & 2 x " (:grocery/purchase-units result))))))
