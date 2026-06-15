(ns pigeon-scoops.production.forms
  (:require [pigeon-scoops.fetchers :refer [post-fetcher!]]
            [pigeon-scoops.hooks :refer [base-url invalidate-production-items!]]))

(defn format-amount [amount amount-unit]
  (when amount
    (str amount (name (keyword amount-unit)))))

(defn index-by-id [id-key entities]
  (->> entities
       (map (fn [entity] [(id-key entity) entity]))
       (into {})))

(defn menu-item-sizes [menus]
  (mapcat (fn [menu] (mapcat :menu-item/sizes (:menu/items menu))) menus))

(defn add-recipe-name [recipes-by-id production-item]
  (assoc production-item
         :recipe/name (get-in recipes-by-id [(:order-item/recipe-id production-item) :recipe/name])))

(defn add-menu-item-size [sizes-by-id production-item]
  (if-let [size (get sizes-by-id (:order-item/menu-item-size-id production-item))]
    (assoc production-item
           :menu-item-size/amount (:menu-item-size/amount size)
           :menu-item-size/amount-unit (:menu-item-size/amount-unit size))
    production-item))

(defn production-items->rows [recipes menus production-items]
  (let [recipes-by-id (index-by-id :recipe/id recipes)
        sizes-by-id (index-by-id :menu-item-size/id (menu-item-sizes menus))]
    (map #(->> %
               (add-recipe-name recipes-by-id)
               (add-menu-item-size sizes-by-id))
         production-items)))

(defn complete-recipe! [token recipe-id]
  (-> (post-fetcher! (str base-url "/production/" recipe-id) {:token token})
      (.then invalidate-production-items!)))
