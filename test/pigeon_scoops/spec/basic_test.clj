(ns pigeon-scoops.spec.basic-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [pigeon-scoops.spec.basic :as bs]))

(t/deftest non-empty-string?-valid
  (t/testing "Non-empty string is true"
    (t/is (s/valid? ::bs/non-empty-string "Foo"))))

(t/deftest non-empty-string?-invalid
  (t/testing "Invalid values are false"
    (t/are [value]
      (false? (s/valid? ::bs/non-empty-string value))
      ""
      42
      []
      :foo
      nil)))
