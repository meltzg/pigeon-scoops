(ns pigeon-scoops.utils.transform-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.utils.transform :as transform]))

(deftest stringify-keyword-test
  (testing "strips the leading colon from a keyword"
    (is (= "unit/cup" (transform/stringify-keyword :unit/cup))))
  (testing "passes non-keyword values through unchanged"
    (is (= "already-a-string" (transform/stringify-keyword "already-a-string")))
    (is (nil? (transform/stringify-keyword nil)))))

(deftest parse-keyword-test
  (testing "parses a string into a keyword"
    (is (= :unit/cup (transform/parse-keyword "unit/cup"))))
  (testing "returns nil for nil input"
    (is (nil? (transform/parse-keyword nil)))))
