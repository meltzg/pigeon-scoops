(ns pigeon-scoops.recipes
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.common :as common]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol])
  (:import (java.util UUID)))

(s/def :recipe/id uuid?)
(s/def :recipe/type #{:recipe/ice-cream :recipe/mixin})
(s/def :recipe/name :basic-spec/non-empty-string)
(s/def :recipe/instructions (s/coll-of :basic-spec/non-empty-string))
(s/def :recipe/amount pos?)
(s/def :recipe/amount-unit (union common/other-units
                                  (set (keys vol/conversion-map))
                                  (set (keys mass/conversion-map))))
(s/def :recipe/source :basic-spec/non-empty-string)

(s/def :recipe/ingredient-type #(= "ingredient" (namespace %)))
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
