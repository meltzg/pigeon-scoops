(ns pigeon-scoops.user-order.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.user-order.forms :as forms]))

(def order-id (random-uuid))
(def order-item-id (random-uuid))
(def order-item-id-2 (random-uuid))
(def recipe-id (random-uuid))
(def recipe-id-2 (random-uuid))
(def new-recipe-id (random-uuid))

(def api-order
  {:user-order/id order-id
   :user-order/note "Birthday party"
   :user-order/items [{:order-item/id order-item-id
                       :order-item/recipe-id recipe-id
                       :order-item/amount 2
                       :order-item/amount-unit :unit/cup
                       :order-item/status :status/draft}]})

(deftest order-data->form-values-test
  (testing "stringifies item dimensions/status and derives an ingredient-id option per item"
    (is (= (update api-order :user-order/items
                   (fn [items]
                     (mapv #(assoc %
                                   :order-item/amount-unit "unit/cup"
                                   :order-item/status "status/draft"
                                   :order-item/ingredient-id (str "recipe:" recipe-id))
                           items)))
           (forms/order-data->form-values api-order)))))

(deftest item-form-values->data-test
  (testing "parses Form values into API-shaped item data"
    (is (= (assoc (-> api-order :user-order/items first)
                  :order-item/ingredient-id (str "recipe:" recipe-id))
           (forms/item-form-values->data
            #js {"order-item/id" order-item-id
                 "order-item/ingredient-id" (str "recipe:" recipe-id)
                 "order-item/amount" 2
                 "order-item/amount-unit" "unit/cup"
                 "order-item/status" "status/draft"})))))

(deftest order-form-values->data-test
  (testing "parses Form values into API-shaped order data"
    (is (= (assoc-in api-order [:user-order/items 0 :order-item/ingredient-id] (str "recipe:" recipe-id))
           (forms/order-form-values->data
            #js {"user-order/id" order-id
                 "user-order/note" "Birthday party"
                 "user-order/items" #js [#js {"order-item/id" order-item-id
                                              "order-item/ingredient-id" (str "recipe:" recipe-id)
                                              "order-item/amount" 2
                                              "order-item/amount-unit" "unit/cup"
                                              "order-item/status" "status/draft"}]})))))

(deftest item->comparable-test
  (testing "narrows a parsed item to the fields relevant for change detection"
    (is (= {:order-item/amount 2
            :order-item/amount-unit :unit/cup
            :order-item/status :status/draft
            :order-item/recipe-id recipe-id}
           (forms/item->comparable
            #js {"order-item/id" order-item-id
                 "order-item/ingredient-id" (str "recipe:" recipe-id)
                 "order-item/amount" 2
                 "order-item/amount-unit" "unit/cup"
                 "order-item/status" "status/draft"}))))

  (testing "includes additional keys (e.g. :order-item/id) when requested"
    (is (= {:order-item/id order-item-id
            :order-item/amount 2
            :order-item/amount-unit :unit/cup
            :order-item/status :status/draft
            :order-item/recipe-id recipe-id}
           (forms/item->comparable
            #js {"order-item/id" order-item-id
                 "order-item/ingredient-id" (str "recipe:" recipe-id)
                 "order-item/amount" 2
                 "order-item/amount-unit" "unit/cup"
                 "order-item/status" "status/draft"}
            [:order-item/id])))))

(deftest order->comparable-test
  (testing "produces an equal comparable form for equivalent API data and form values"
    (is (= (forms/order->comparable api-order)
           (forms/order->comparable
            #js {"user-order/id" order-id
                 "user-order/note" "Birthday party"
                 "user-order/items" #js [#js {"order-item/id" order-item-id
                                              "order-item/ingredient-id" (str "recipe:" recipe-id)
                                              "order-item/amount" 2
                                              "order-item/amount-unit" "unit/cup"
                                              "order-item/status" "status/draft"}]}))))

  (testing "excludes identifying fields like :user-order/id and :order-item/id"
    (is (not (contains? (forms/order->comparable api-order) :user-order/id)))
    (is (not (contains? (first (:user-order/items (forms/order->comparable api-order)))
                        :order-item/id)))))

(deftest order-save-ops-test
  (testing "classifies items as new/update/delete relative to the initial order"
    (let [initial-order (update api-order :user-order/items conj
                                {:order-item/id order-item-id-2
                                 :order-item/recipe-id recipe-id-2
                                 :order-item/amount 1
                                 :order-item/amount-unit :unit/each
                                 :order-item/status :status/draft})
          {:keys [order order-item-ops]}
          (forms/order-save-ops
           initial-order
           #js {"user-order/id" order-id
                "user-order/note" "Birthday party"
                "user-order/items"
                #js [#js {"order-item/id" order-item-id
                          "order-item/ingredient-id" (str "recipe:" recipe-id)
                          "order-item/amount" 3
                          "order-item/amount-unit" "unit/cup"
                          "order-item/status" "status/draft"}
                     #js {"order-item/ingredient-id" (str "recipe:" new-recipe-id)
                          "order-item/amount" 1
                          "order-item/amount-unit" "unit/each"
                          "order-item/status" "status/draft"}]})]
      (is (= order-id (:user-order/id order)))
      (is (= [{:order-item/recipe-id new-recipe-id
               :order-item/amount 1
               :order-item/amount-unit :unit/each
               :order-item/status :status/draft}]
             (vec (:new order-item-ops))))
      (is (= [{:order-item/id order-item-id
               :order-item/recipe-id recipe-id
               :order-item/amount 3
               :order-item/amount-unit :unit/cup
               :order-item/status :status/draft}]
             (vec (:update order-item-ops))))
      (is (= #{order-item-id-2} (set (:delete order-item-ops)))))))
