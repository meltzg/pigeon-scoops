(ns pigeon-scoops.basic-spec-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [pigeon-scoops.basic-spec]))

(deftest non-empty-string?-valid
  (testing "Non-empty string is true"
    (is (s/valid? :basic-spec/non-empty-string "Foo"))))

(deftest non-empty-string?-invalid
  (testing "Invalid values are false"
    (are [value] (false? (s/valid? :basic-spec/non-empty-string value))
                 ""
                 42
                 []
                 :foo
                 nil)))
