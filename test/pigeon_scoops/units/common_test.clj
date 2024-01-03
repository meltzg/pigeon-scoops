(ns pigeon-scoops.units.common-test
  (:require [clojure.test :as t]
            [pigeon-scoops.common-test :refer [tolerance]]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(t/deftest convert-test
  (t/testing "can convert units correctly and can be converted back"
    (t/are [val from to expected]
      (and (< (abs (- expected (units/convert val from to)))
              tolerance)
           (< (abs (- val (units/convert (units/convert val from to) to from)))
              tolerance))
      ;; mass conversion
      1 ::mass/lb ::mass/oz 16
      2 ::mass/kg ::mass/lb 4.40925
      3 ::mass/kg ::mass/g 3000
      4000 ::mass/mg ::mass/g 4
      ;; volume conversion
      1 ::vol/gal ::vol/qt 4
      1 ::vol/qt ::vol/pt 2
      1 ::vol/pt ::vol/c 2
      1 ::vol/c ::vol/floz 8
      1 ::vol/floz ::vol/tbsp 2
      1 ::vol/tbsp ::vol/tsp 3
      768 ::vol/tsp ::vol/gal 1
      1 ::vol/l ::vol/ml 1000
      1 ::vol/l ::vol/gal 0.2642
      2 ::units/unit ::units/unit 2)))

(t/deftest convert-invalid-test
  (t/testing "invalid conversions return nil"
    (t/are [from to]
      (nil? (units/convert 8 from to))
      ::mass/oz ::units/pinch
      ::units/pinch ::mass/oz
      ::vol/tbsp ::units/pinch
      ::units/pinch ::vol/tbsp
      ::vol/c ::mass/g
      ::mass/kg ::vol/floz
      ::units/pinch ::units/unit)))

(t/deftest scale-factor-test
  (t/testing "can find scale factor from one amount in one unit to another amount in another unit"
    (t/are [amount-from unit-from amount-to unit-to expected]
      (let [actual (units/scale-factor amount-from unit-from amount-to unit-to)]
        (if (pos? expected)
          (< (abs (- expected actual)) tolerance)
          (nil? actual)))
      1 ::vol/qt 2 ::vol/gal 8.0
      1 ::vol/qt 0.125 ::vol/gal 0.5
      2 ::vol/l 5 ::vol/c 2.36588
      2 ::mass/lb 4 ::mass/oz 0.5
      2 ::mass/lb 2 ::vol/l -1
      2 ::vol/c 2 ::mass/g -1
      2 ::units/pinch 3 ::units/pinch 1.5
      2 ::units/pinch 3 ::units/unit -1
      2 ::units/pinch 3 ::vol/c -1
      2 ::vol/c 3 ::units/unit -1)))

(t/deftest to-comparable-test
  (t/testing "can convert an amount to it's comparable unit amount"
    (t/are [val unit expected]
      (let [actual (units/to-comparable val unit)]
        (if expected
          (< (abs (- expected actual)) tolerance)
          (nil? actual)))
      2 ::units/unit 2
      3 ::mass/oz (units/convert 3 ::mass/oz ::mass/g)
      4 ::vol/gal (units/convert 4 ::vol/gal ::vol/ml))))

(t/deftest to-unit-class-test
  (t/testing "can convert a unit type to a string representing what class of unit"
    (t/are [unit-type expected]
      (= (units/to-unit-class unit-type) expected)
      ::units/pinch "common"
      ::vol/c "volume"
      ::mass/g "mass")))
