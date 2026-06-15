(ns pigeon-scoops.utils.entity-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.utils.entity :as entity]))

(defn- normalize
  "determine-ops omits :new/:update keys when empty and may return lazy seqs;
   normalize the shape so expectations can be written as plain vectors."
  [ops]
  (-> (merge {:new [] :update [] :delete #{}} ops)
      (update :new vec)
      (update :update vec)))

(deftest determine-ops-test
  (testing "empty original and updated collections produce no ops"
    (is (= {:new [] :update [] :delete #{}}
           (normalize (entity/determine-ops :id [] [])))))

  (testing "entities without a matching original id are :new"
    (is (= {:new [{:id 1 :name "a"}] :update [] :delete #{}}
           (normalize (entity/determine-ops :id [] [{:id 1 :name "a"}])))))

  (testing "entities present in both but unchanged are excluded from :update"
    (is (= {:new [] :update [] :delete #{}}
           (normalize (entity/determine-ops :id [{:id 1 :name "a"}] [{:id 1 :name "a"}])))))

  (testing "entities present in both but changed are :update"
    (is (= {:new [] :update [{:id 1 :name "b"}] :delete #{}}
           (normalize (entity/determine-ops :id [{:id 1 :name "a"}] [{:id 1 :name "b"}])))))

  (testing "ids missing from updated are reported as :delete"
    (is (= {:new [] :update [] :delete #{1}}
           (normalize (entity/determine-ops :id [{:id 1 :name "a"}] [])))))

  (testing "mixes new, updated, unchanged and deleted entities together"
    (is (= {:new [{:id 3 :name "c"}]
            :update [{:id 2 :name "b2"}]
            :delete #{1}}
           (normalize (entity/determine-ops :id
                                            [{:id 1 :name "a"} {:id 2 :name "b"}]
                                            [{:id 2 :name "b2"} {:id 3 :name "c"}])))))

  (testing "make-comparable normalizes entities so unrelated field changes don't trigger :update"
    (is (= {:new [] :update [] :delete #{}}
           (normalize (entity/determine-ops :id
                                            [{:id 1 :name "a" :ignored-field "x"}]
                                            [{:id 1 :name "a" :ignored-field "y"}]
                                            #(dissoc % :ignored-field))))))

  (testing "id-key is read from the comparable form, so stripping it makes every entity look :new"
    (is (= {:new [{:name "b"}] :update [] :delete #{}}
           (normalize (entity/determine-ops :id
                                            [{:id 1 :name "a"}]
                                            [{:id 1 :name "b"}]
                                            #(dissoc % :id)))))))
