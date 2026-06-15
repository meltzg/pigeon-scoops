(ns pigeon-scoops.storefront.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.storefront.forms :as forms]))

(def menu-id (random-uuid))
(def menu-item-id (random-uuid))
(def menu-item-size-id (random-uuid))
(def recipe-id (random-uuid))
(def order-id (random-uuid))
(def order-item-id (random-uuid))

(def menus
  [{:menu/id menu-id
    :menu/end-time "2026-06-10"
    :menu/items [{:menu-item/id menu-item-id
                  :menu-item/menu-id menu-id
                  :menu-item/recipe-id recipe-id
                  :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                                     :menu-item-size/amount 2
                                     :menu-item-size/amount-unit :unit/cup}]}]}])

(def active-order
  {:user-order/id order-id
   :user-order/items [{:order-item/id order-item-id
                       :order-item/menu-item-size-id menu-item-size-id
                       :order-item/amount 4
                       :order-item/amount-unit :unit/cup}]})

(deftest menu-order-data->storefront-form-values-test
  (testing "derives end-time and order-quantity (existing amount / size amount) per size"
    (is (= {:storefront/items
            [{:menu-item/id menu-item-id
              :menu-item/menu-id menu-id
              :menu-item/recipe-id recipe-id
              :menu-item/end-time "2026-06-10"
              :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                                 :menu-item-size/amount 2
                                 :menu-item-size/amount-unit "unit/cup"
                                 :menu-item-size/order-quantity 2}]}]}
           (forms/menu-order-data->storefront-form-values menus active-order))))

  (testing "defaults order-quantity to zero when there is no matching order item"
    (is (= 0
           (-> (forms/menu-order-data->storefront-form-values menus nil)
               :storefront/items
               first
               :menu-item/sizes
               first
               :menu-item-size/order-quantity)))))

(deftest item-size-form-value->data-test
  (testing "parses the amount-unit keyword back from a string"
    (is (= {:menu-item-size/id menu-item-size-id
            :menu-item-size/amount 2
            :menu-item-size/amount-unit :unit/cup
            :menu-item-size/order-quantity 2}
           (forms/item-size-form-value->data
            #js {"menu-item-size/id" menu-item-size-id
                 "menu-item-size/amount" 2
                 "menu-item-size/amount-unit" "unit/cup"
                 "menu-item-size/order-quantity" 2})))))

(deftest storefront-form-values->data-test
  (testing "parses Form values into API-shaped storefront data"
    (is (= {:storefront/items
            [{:menu-item/id menu-item-id
              :menu-item/menu-id menu-id
              :menu-item/recipe-id recipe-id
              :menu-item/end-time "2026-06-10"
              :menu-item/sizes [{:menu-item-size/id menu-item-size-id
                                 :menu-item-size/amount 2
                                 :menu-item-size/amount-unit :unit/cup
                                 :menu-item-size/order-quantity 2}]}]}
           (update (forms/storefront-form-values->data
                    #js {"storefront/items" #js [#js {"menu-item/id" menu-item-id
                                                      "menu-item/menu-id" menu-id
                                                      "menu-item/recipe-id" recipe-id
                                                      "menu-item/end-time" "2026-06-10"
                                                      "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                                                                  "menu-item-size/amount" 2
                                                                                  "menu-item-size/amount-unit" "unit/cup"
                                                                                  "menu-item-size/order-quantity" 2}]}]})
                   :storefront/items
                   (fn [items] (mapv #(update % :menu-item/sizes vec) items)))))))

(deftest storefront-save-ops-test
  (testing "indexes existing order items by menu-item-size-id and tags each size with its parent ids"
    (let [{:keys [size->order-item storefront-sizes]}
          (forms/storefront-save-ops
           active-order
           #js {"storefront/items" #js [#js {"menu-item/id" menu-item-id
                                             "menu-item/menu-id" menu-id
                                             "menu-item/recipe-id" recipe-id
                                             "menu-item/end-time" "2026-06-10"
                                             "menu-item/sizes" #js [#js {"menu-item-size/id" menu-item-size-id
                                                                         "menu-item-size/amount" 2
                                                                         "menu-item-size/amount-unit" "unit/cup"
                                                                         "menu-item-size/order-quantity" 3}]}]})]
      (is (= {menu-item-size-id (-> active-order :user-order/items first)}
             size->order-item))
      (is (= [{:menu-item-size/id menu-item-size-id
               :menu-item-size/amount 2
               :menu-item-size/amount-unit :unit/cup
               :menu-item-size/order-quantity 3
               :menu-item-size/menu-item-id menu-item-id
               :menu-item-size/menu-id menu-id
               :menu-item-size/recipe-id recipe-id}]
             (vec storefront-sizes))))))
