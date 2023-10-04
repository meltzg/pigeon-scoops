(ns pigeon-scoops.units.mass-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.units.mass :as mass]))

(deftest convert-test
  (testing "can convert units correctly and can be converted back"
    (are [val from to expected] (and (< (abs (- expected (mass/convert val from to)))
                                        0.0001)
                                     (< (abs (- val (mass/convert (mass/convert val from to) to from)))
                                        0.0001))
                                1 :mass/lb :mass/oz 16
                                2 :mass/kg :mass/lb 4.40925
                                3 :mass/kg :mass/g 3000
                                4000 :mass/mg :mass/g 4)))

(deftest convert-invalid-test
  (testing "invalid conversions return nil"
    (are [from to] (nil? (mass/convert 8 from to))
                   :mass/oz :other/pinch
                   :other/pinch :mass/oz)))
