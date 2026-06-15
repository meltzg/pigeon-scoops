(ns pigeon-scoops.production.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.production.forms :as forms]))

(def recipe-id (random-uuid))
(def other-recipe-id (random-uuid))
(def menu-id (random-uuid))
(def menu-item-id (random-uuid))
(def menu-item-size-id (random-uuid))

(def recipes
  [{:recipe/id recipe-id :recipe/name "Vanilla"}])

(def menu-item-size
  {:menu-item-size/id menu-item-size-id
   :menu-item-size/menu-item-id menu-item-id
   :menu-item-size/amount 12
   :menu-item-size/amount-unit :volume/oz})

(def menus
  [{:menu/id menu-id
    :menu/items [{:menu-item/id menu-item-id
                  :menu-item/menu-id menu-id
                  :menu-item/recipe-id recipe-id
                  :menu-item/sizes [menu-item-size]}]}])

(deftest format-amount-test
  (testing "joins amount and unit name"
    (is (= "12oz" (forms/format-amount 12 :volume/oz))))
  (testing "returns nil when amount is missing"
    (is (nil? (forms/format-amount nil :volume/oz)))))

(deftest index-by-id-test
  (testing "indexes entities by the value at the given id key"
    (is (= {recipe-id (first recipes)}
           (forms/index-by-id :recipe/id recipes))))
  (testing "returns an empty map for an empty collection"
    (is (= {} (forms/index-by-id :recipe/id [])))))

(deftest menu-item-sizes-test
  (testing "flattens sizes across every item of every menu"
    (is (= [menu-item-size]
           (forms/menu-item-sizes menus))))
  (testing "returns an empty seq when there are no menus"
    (is (empty? (forms/menu-item-sizes [])))))

(deftest add-recipe-name-test
  (let [recipes-by-id (forms/index-by-id :recipe/id recipes)]
    (testing "looks up and assocs the recipe name by recipe-id"
      (is (= "Vanilla"
             (:recipe/name (forms/add-recipe-name recipes-by-id
                                                  {:order-item/recipe-id recipe-id})))))
    (testing "assocs nil when the recipe cannot be found"
      (is (nil? (:recipe/name (forms/add-recipe-name recipes-by-id
                                                     {:order-item/recipe-id other-recipe-id})))))))

(deftest add-menu-item-size-test
  (let [sizes-by-id (forms/index-by-id :menu-item-size/id (forms/menu-item-sizes menus))]
    (testing "assocs the size's amount and unit by menu-item-size-id"
      (is (= {:order-item/menu-item-size-id menu-item-size-id
              :menu-item-size/amount 12
              :menu-item-size/amount-unit :volume/oz}
             (forms/add-menu-item-size sizes-by-id {:order-item/menu-item-size-id menu-item-size-id}))))
    (testing "leaves the item untouched when it has no menu-item-size-id"
      (is (= {:order-item/recipe-id recipe-id}
             (forms/add-menu-item-size sizes-by-id {:order-item/recipe-id recipe-id}))))))

(deftest production-items->rows-test
  (testing "joins each item with its recipe name and, when present, its size details"
    (is (= [{:order-item/recipe-id recipe-id
             :order-item/amount 24
             :order-item/amount-unit :volume/oz
             :order-item/menu-item-size-id menu-item-size-id
             :order-item/menu-item-size-quantity 2
             :recipe/name "Vanilla"
             :menu-item-size/amount 12
             :menu-item-size/amount-unit :volume/oz}]
           (forms/production-items->rows
            recipes menus
            [{:order-item/recipe-id recipe-id
              :order-item/amount 24
              :order-item/amount-unit :volume/oz
              :order-item/menu-item-size-id menu-item-size-id
              :order-item/menu-item-size-quantity 2}]))))

  (testing "still adds the recipe name when items carry no size (sizes not separated)"
    (is (= [{:order-item/recipe-id recipe-id
             :order-item/amount 24
             :order-item/amount-unit :volume/oz
             :recipe/name "Vanilla"}]
           (forms/production-items->rows
            recipes []
            [{:order-item/recipe-id recipe-id
              :order-item/amount 24
              :order-item/amount-unit :volume/oz}])))))
