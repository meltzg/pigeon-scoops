(ns pigeon-scoops.utils.table-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.utils.table :as table]))

(deftest make-sorter-test
  (testing "returns a function that compares two JS objects by the given key"
    (let [sorter (table/make-sorter :name)]
      (is (neg? (sorter #js {:name "Alice"} #js {:name "Bob"})))
      (is (pos? (sorter #js {:name "Bob"} #js {:name "Alice"})))
      (is (zero? (sorter #js {:name "Alice"} #js {:name "Alice"})))))

  (testing "sorts numeric values correctly"
    (let [sorter (table/make-sorter :amount)]
      (is (neg? (sorter #js {:amount 1} #js {:amount 2})))
      (is (pos? (sorter #js {:amount 10} #js {:amount 2}))))))
