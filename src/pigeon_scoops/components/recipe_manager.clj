(ns pigeon-scoops.components.recipe-manager
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec :as bs]
            [pigeon-scoops.components.grocery-manager :as g]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol])
  (:import (java.util UUID)))

(s/def ::id uuid?)
(s/def ::type #{::ice-cream ::sorbet ::mixin})
(s/def ::name ::bs/non-empty-string)
(s/def ::instructions (s/coll-of ::bs/non-empty-string))
(s/def ::amount pos?)
(s/def ::amount-unit (union units/other-units
                            (set (keys vol/conversion-map))
                            (set (keys mass/conversion-map))))
(s/def ::source ::bs/non-empty-string)

(s/def ::ingredient-type ::g/type)
(s/def ::ingredient (s/keys :req [::ingredient-type
                                  ::amount
                                  ::amount-unit]))
(s/def ::ingredients (s/coll-of ::ingredient))

(s/def ::mixin (s/keys :req [::id
                             ::amount
                             ::amount-unit]))

(s/def ::mixins (s/coll-of ::mixin))

(s/def ::entry (s/keys :req [::id
                             ::type
                             ::name
                             ::instructions
                             ::amount
                             ::amount-unit
                             ::ingredients]
                       :opt [::source
                             ::mixins]))

(defn add-recipe [recipes new-recipe]
  (let [recipe-id (or (::id new-recipe)
                      (UUID/randomUUID))
        conformed-recipe (s/conform ::entry (assoc new-recipe ::id recipe-id))]
    (if (s/invalid? conformed-recipe)
      recipes
      (conj (remove #(= (::id %) recipe-id) recipes) conformed-recipe))))

(defn scale-recipe [recipe amount amount-unit]
  (let [scale-factor (units/scale-factor (::amount recipe)
                                         (::amount-unit recipe)
                                         amount
                                         amount-unit)]
    (-> recipe
        (update ::ingredients (partial map #(update % ::amount * scale-factor)))
        (update ::mixins (partial map #(update % ::amount * scale-factor)))
        (assoc ::amount amount
               ::amount-unit amount-unit))))


(defn materialize-mixins [recipe-to-materialize recipe-library]
  (loop [to-materialize (::mixins recipe-to-materialize)
         materialized-mixins []]
    (if (empty? to-materialize)
      materialized-mixins
      (let [mixin-recipes (map (fn [mixin]
                                 (scale-recipe (->> recipe-library
                                                    (filter #(= (::id %) (::id mixin)))
                                                    first)
                                               (::amount mixin)
                                               (::amount-unit mixin)))
                               to-materialize)
            new-mixins (mapcat ::mixins mixin-recipes)]
        (recur new-mixins (concat materialized-mixins mixin-recipes))))))

(defn merge-recipe-ingredients [recipes-to-merge recipe-library]
  (->> (mapcat ::ingredients recipes-to-merge)
       (concat (mapcat #(mapcat ::ingredients (materialize-mixins % recipe-library)) recipes-to-merge))
       (group-by #(list (::ingredient-type %) (namespace (::amount-unit %))))
       vals
       (map #(reduce (fn [acc ingredient]
                       (update acc ::amount + (units/convert (::amount ingredient)
                                                             (::amount-unit ingredient)
                                                             (::amount-unit acc)))) %))))

(defn to-grocery-purchase-list [recipe-ingredients groceries]
  (let [grocery-map (into {} (map #(vec [(::g/type %) %]) groceries))
        purchase-list (map #(g/divide-grocery (::amount %)
                                              (::amount-unit %)
                                              ((::ingredient-type %) grocery-map))
                           recipe-ingredients)]
    {:purchase-list purchase-list
     :total-cost    (apply + (map #(* (::g/unit-cost %) (::g/unit-purchase-quantity %))
                                  (mapcat ::g/units purchase-list)))}))
