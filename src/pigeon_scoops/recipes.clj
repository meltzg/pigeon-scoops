(ns pigeon-scoops.recipes
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.groceries]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol])
  (:import (java.util UUID)))

(s/def :recipe/id uuid?)
(s/def :recipe/type #{:recipe/ice-cream :recipe/mixin})
(s/def :recipe/name :basic-spec/non-empty-string)
(s/def :recipe/instructions (s/coll-of :basic-spec/non-empty-string))
(s/def :recipe/amount pos?)
(s/def :recipe/amount-unit (union units/other-units
                                  (set (keys vol/conversion-map))
                                  (set (keys mass/conversion-map))))
(s/def :recipe/source :basic-spec/non-empty-string)

(s/def :recipe/ingredient-type :grocery/type)
(s/def :recipe/ingredient (s/keys :req [:recipe/ingredient-type
                                        :recipe/amount
                                        :recipe/amount-unit]))
(s/def :recipe/ingredients (s/coll-of :recipe/ingredient))

(s/def :recipe/entry (s/keys :req [:recipe/id
                                   :recipe/type
                                   :recipe/name
                                   :recipe/instructions
                                   :recipe/amount
                                   :recipe/amount-unit
                                   :recipe/ingredients]
                             :opt [:recipe/source]))

(defn add-recipe [recipes new-recipe]
  (let [recipe-id (or (:recipe/id new-recipe)
                      (UUID/randomUUID))
        conformed-recipe (s/conform :recipe/entry (assoc new-recipe :recipe/id recipe-id))]
    (if (s/invalid? conformed-recipe)
      recipes
      (conj (remove #(= (:recipe/id %) recipe-id) recipes) conformed-recipe))))

(defn scale-recipe [recipe amount amount-unit]
  (let [scale-factor (units/scale-factor (:recipe/amount recipe)
                                         (:recipe/amount-unit recipe)
                                         amount
                                         amount-unit)]
    (assoc (update recipe
                   :recipe/ingredients
                   (partial map #(update % :recipe/amount * scale-factor)))
      :recipe/amount amount
      :recipe/amount-unit amount-unit)))

(defn merge-recipe-ingredients [recipes]
  (->> (mapcat :recipe/ingredients recipes)
       (group-by #(list (:recipe/ingredient-type %) (namespace (:recipe/amount-unit %))))
       vals
       (map #(reduce (fn [acc ingredient]
                       (update acc :recipe/amount + (units/convert (:recipe/amount ingredient)
                                                                   (:recipe/amount-unit ingredient)
                                                                   (:recipe/amount-unit acc)))) %))))
