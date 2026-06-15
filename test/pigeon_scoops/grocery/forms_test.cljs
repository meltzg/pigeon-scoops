(ns pigeon-scoops.grocery.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.grocery.forms :as forms]))

(def grocery-id (random-uuid))
(def unit-id (random-uuid))
(def unit-id-2 (random-uuid))

(def api-grocery
  {:grocery/id grocery-id
   :grocery/name "Flour"
   :grocery/department :department/baking
   :grocery/units [{:grocery-unit/id unit-id
                    :grocery-unit/source "Costco"
                    :grocery-unit/unit-cost 10.0
                    :grocery-unit/unit-mass 5
                    :grocery-unit/unit-mass-type :unit/lb
                    :grocery-unit/unit-volume nil
                    :grocery-unit/unit-volume-type nil
                    :grocery-unit/unit-common nil
                    :grocery-unit/unit-common-type nil}]})

(deftest grocery-data->form-values-test
  (testing "stringifies the department and each unit's dimension types"
    (is (= (-> api-grocery
               (assoc :grocery/department "department/baking")
               (assoc-in [:grocery/units 0 :grocery-unit/unit-mass-type] "unit/lb"))
           (forms/grocery-data->form-values api-grocery)))))

(deftest unit-form-values->data-test
  (testing "parses the dimension type keywords back from strings"
    (is (= (-> api-grocery :grocery/units first)
           (forms/unit-form-values->data
            #js {"grocery-unit/id" unit-id
                 "grocery-unit/source" "Costco"
                 "grocery-unit/unit-cost" 10.0
                 "grocery-unit/unit-mass" 5
                 "grocery-unit/unit-mass-type" "unit/lb"
                 "grocery-unit/unit-volume" nil
                 "grocery-unit/unit-volume-type" nil
                 "grocery-unit/unit-common" nil
                 "grocery-unit/unit-common-type" nil})))))

(deftest grocery-form-values->data-test
  (testing "parses Form values into API-shaped grocery data"
    (is (= api-grocery
           (forms/grocery-form-values->data
            #js {"grocery/id" grocery-id
                 "grocery/name" "Flour"
                 "grocery/department" "department/baking"
                 "grocery/units" #js [#js {"grocery-unit/id" unit-id
                                           "grocery-unit/source" "Costco"
                                           "grocery-unit/unit-cost" 10.0
                                           "grocery-unit/unit-mass" 5
                                           "grocery-unit/unit-mass-type" "unit/lb"
                                           "grocery-unit/unit-volume" nil
                                           "grocery-unit/unit-volume-type" nil
                                           "grocery-unit/unit-common" nil
                                           "grocery-unit/unit-common-type" nil}]})))))

(deftest unit->comparable-test
  (testing "selects populated dimension fields and drops nil values"
    (is (= {:grocery-unit/source "Costco"
            :grocery-unit/unit-cost 10.0
            :grocery-unit/unit-mass 5
            :grocery-unit/unit-mass-type :unit/lb}
           (forms/unit->comparable (-> api-grocery :grocery/units first)))))

  (testing "includes additional keys when present"
    (is (= {:grocery-unit/id unit-id
            :grocery-unit/source "Costco"
            :grocery-unit/unit-cost 10.0
            :grocery-unit/unit-mass 5
            :grocery-unit/unit-mass-type :unit/lb}
           (forms/unit->comparable (-> api-grocery :grocery/units first) [:grocery-unit/id])))))

(deftest grocery->comparable-test
  (testing "produces an equal comparable form for equivalent API data and form values"
    (is (= (forms/grocery->comparable api-grocery)
           (forms/grocery->comparable
            #js {"grocery/id" grocery-id
                 "grocery/name" "Flour"
                 "grocery/department" "department/baking"
                 "grocery/units" #js [#js {"grocery-unit/id" unit-id
                                           "grocery-unit/source" "Costco"
                                           "grocery-unit/unit-cost" 10.0
                                           "grocery-unit/unit-mass" 5
                                           "grocery-unit/unit-mass-type" "unit/lb"}]}))))

  (testing "excludes identifying fields like :grocery/id and :grocery-unit/id"
    (is (not (contains? (forms/grocery->comparable api-grocery) :grocery/id)))
    (is (not (contains? (first (:grocery/units (forms/grocery->comparable api-grocery)))
                        :grocery-unit/id)))))

(deftest grocery-save-ops-test
  (testing "classifies units as new/update/delete relative to the initial grocery"
    (let [initial-grocery (update api-grocery :grocery/units conj
                                  {:grocery-unit/id unit-id-2
                                   :grocery-unit/source "Sam's Club"
                                   :grocery-unit/unit-cost 8.0
                                   :grocery-unit/unit-mass 10
                                   :grocery-unit/unit-mass-type :unit/lb
                                   :grocery-unit/unit-volume nil
                                   :grocery-unit/unit-volume-type nil
                                   :grocery-unit/unit-common nil
                                   :grocery-unit/unit-common-type nil})
          {:keys [grocery grocery-unit-ops]}
          (forms/grocery-save-ops
           initial-grocery
           #js {"grocery/id" grocery-id
                "grocery/name" "Flour"
                "grocery/department" "department/baking"
                "grocery/units"
                #js [#js {"grocery-unit/id" unit-id
                          "grocery-unit/source" "Costco"
                          "grocery-unit/unit-cost" 12.0
                          "grocery-unit/unit-mass" 5
                          "grocery-unit/unit-mass-type" "unit/lb"}
                     #js {"grocery-unit/source" "Walmart"
                          "grocery-unit/unit-cost" 9.0
                          "grocery-unit/unit-mass" 5
                          "grocery-unit/unit-mass-type" "unit/lb"}]})]
      (is (= grocery-id (:grocery/id grocery)))
      (is (= [{:grocery-unit/source "Walmart"
               :grocery-unit/unit-cost 9.0
               :grocery-unit/unit-mass 5
               :grocery-unit/unit-mass-type :unit/lb}]
             (vec (:new grocery-unit-ops))))
      (is (= [{:grocery-unit/id unit-id
               :grocery-unit/source "Costco"
               :grocery-unit/unit-cost 12.0
               :grocery-unit/unit-mass 5
               :grocery-unit/unit-mass-type :unit/lb}]
             (vec (:update grocery-unit-ops))))
      (is (= #{unit-id-2} (set (:delete grocery-unit-ops)))))))
