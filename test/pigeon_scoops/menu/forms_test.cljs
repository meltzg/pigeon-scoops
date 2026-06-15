(ns pigeon-scoops.menu.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.menu.forms :as forms]))

(def menu-id (random-uuid))
(def menu-item-id (random-uuid))
(def menu-item-id-2 (random-uuid))
(def menu-item-size-id (random-uuid))
(def menu-item-size-id-2 (random-uuid))
(def recipe-id (random-uuid))
(def recipe-id-2 (random-uuid))
(def new-recipe-id (random-uuid))

(def api-menu
  {:menu/id menu-id
   :menu/name "Summer Menu"
   :menu/active true
   :menu/repeats false
   :menu/duration 7
   :menu/duration-type :duration/days
   :menu/items [{:menu-item/id menu-item-id
                 :menu-item/recipe-id recipe-id
                 :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                                    :menu-item-size/amount 1
                                    :menu-item-size/amount-unit :unit/each}]}]})

(deftest menu-data->form-values-test
  (testing "stringifies the duration type, derives ingredient-id options, and adds limited-quantity? per size"
    (is (= (-> api-menu
               (assoc :menu/duration-type "duration/days")
               (assoc-in [:menu/items 0 :menu-item/ingredient-id] (str "recipe:" recipe-id))
               (assoc-in [:menu/items 0 :menu-item/sizes 0 :menu-item-size/limited-quantity?] false))
           (update (forms/menu-data->form-values api-menu)
                   :menu/items (fn [items] (mapv #(update % :menu-item/sizes vec) items)))))))

(deftest menu-form-values->data-test
  (testing "parses Form values into API-shaped menu data"
    (is (= (assoc-in api-menu [:menu/items 0 :menu-item/ingredient-id] (str "recipe:" recipe-id))
           (forms/menu-form-values->data
            #js {"menu/id" menu-id
                 "menu/name" "Summer Menu"
                 "menu/active" true
                 "menu/repeats" false
                 "menu/duration" 7
                 "menu/duration-type" "duration/days"
                 "menu/items" #js [#js {"menu-item/id" menu-item-id
                                        "menu-item/ingredient-id" (str "recipe:" recipe-id)
                                        "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                                                    "menu-item-size/amount" 1
                                                                    "menu-item-size/amount-unit" "unit/each"}]}]})))))

(deftest item-size->comparable-test
  (testing "selects amount fields and drops nil values"
    (is (= {:menu-item-size/amount 1 :menu-item-size/amount-unit :unit/each}
           (forms/item-size->comparable {:menu-item-size/id menu-item-size-id
                                         :menu-item-size/amount 1
                                         :menu-item-size/amount-unit :unit/each})))
    (is (= {:menu-item-size/amount-unit :unit/each}
           (forms/item-size->comparable {:menu-item-size/amount nil
                                         :menu-item-size/amount-unit :unit/each}))))

  (testing "includes additional keys when present"
    (is (= {:menu-item-size/id menu-item-size-id
            :menu-item-size/amount 1
            :menu-item-size/amount-unit :unit/each}
           (forms/item-size->comparable {:menu-item-size/id menu-item-size-id
                                         :menu-item-size/amount 1
                                         :menu-item-size/amount-unit :unit/each}
                                        [:menu-item-size/id])))))

(deftest item->comparable-test
  (testing "narrows a parsed item to recipe-id and comparable sizes"
    (is (= {:menu-item/recipe-id recipe-id
            :menu-item/sizes [{:menu-item-size/amount 1 :menu-item-size/amount-unit :unit/each}]}
           (forms/item->comparable
            #js {"menu-item/id" menu-item-id
                 "menu-item/ingredient-id" (str "recipe:" recipe-id)
                 "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                             "menu-item-size/amount" 1
                                             "menu-item-size/amount-unit" "unit/each"}]}))))

  (testing "includes additional keys on both the item and its sizes"
    (is (= {:menu-item/id menu-item-id
            :menu-item/recipe-id recipe-id
            :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                               :menu-item-size/amount 1
                               :menu-item-size/amount-unit :unit/each}]}
           (forms/item->comparable
            #js {"menu-item/id" menu-item-id
                 "menu-item/ingredient-id" (str "recipe:" recipe-id)
                 "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                             "menu-item-size/amount" 1
                                             "menu-item-size/amount-unit" "unit/each"}]}
            [:menu-item/id :menu-item-size/id])))))

(deftest menu->comparable-test
  (testing "produces an equal comparable form for equivalent API data and form values"
    (is (= (forms/menu->comparable api-menu)
           (forms/menu->comparable
            #js {"menu/id" menu-id
                 "menu/name" "Summer Menu"
                 "menu/active" true
                 "menu/repeats" false
                 "menu/duration" 7
                 "menu/duration-type" "duration/days"
                 "menu/items" #js [#js {"menu-item/id" menu-item-id
                                        "menu-item/ingredient-id" (str "recipe:" recipe-id)
                                        "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                                                    "menu-item-size/amount" 1
                                                                    "menu-item-size/amount-unit" "unit/each"}]}]}))))

  (testing "excludes identifying fields like :menu/id, :menu-item/id and :menu-item-size/id"
    (let [comparable (forms/menu->comparable api-menu)]
      (is (not (contains? comparable :menu/id)))
      (is (not (contains? (first (:menu/items comparable)) :menu-item/id)))
      (is (not (contains? (first (:menu-item/sizes (first (:menu/items comparable)))) :menu-item-size/id))))))

(deftest menu-save-ops-test
  (testing "classifies items as new/update/delete relative to the initial menu"
    (let [initial-menu (update api-menu :menu/items conj
                               {:menu-item/id menu-item-id-2
                                :menu-item/recipe-id recipe-id-2
                                :menu-item/sizes [{:menu-item-size/id menu-item-size-id-2
                                                   :menu-item-size/amount 1
                                                   :menu-item-size/amount-unit :unit/each}]})
          {:keys [menu menu-item-ops]}
          (forms/menu-save-ops
           initial-menu
           #js {"menu/id" menu-id
                "menu/name" "Summer Menu"
                "menu/active" true
                "menu/repeats" false
                "menu/duration" 7
                "menu/duration-type" "duration/days"
                "menu/items"
                #js [#js {"menu-item/id" menu-item-id
                          "menu-item/ingredient-id" (str "recipe:" recipe-id)
                          "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                                      "menu-item-size/amount" 2
                                                      "menu-item-size/amount-unit" "unit/each"}]}
                     #js {"menu-item/ingredient-id" (str "recipe:" new-recipe-id)
                          "menu-item/sizes" #js [#js {"menu-item-size/amount" 1
                                                      "menu-item-size/amount-unit" "unit/each"}]}]})]
      (is (= menu-id (:menu/id menu)))
      (is (= [{:menu-item/recipe-id new-recipe-id
               :menu-item/sizes [{:menu-item-size/amount 1 :menu-item-size/amount-unit :unit/each}]}]
             (vec (:new menu-item-ops))))
      (is (= [{:menu-item/id menu-item-id
               :menu-item/recipe-id recipe-id
               :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                                  :menu-item-size/amount 2
                                  :menu-item-size/amount-unit :unit/each}]}]
             (vec (:update menu-item-ops))))
      (is (= #{menu-item-id-2} (set (:delete menu-item-ops)))))))
