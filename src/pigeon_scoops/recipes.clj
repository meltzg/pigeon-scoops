(ns pigeon-scoops.recipes
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.other :as other]
            [pigeon-scoops.units.volume :as vol]))

(s/def :recipe/id uuid?)
(s/def :recipe/name :basic-spec/non-empty-string)
(s/def :recipe/instructions (s/coll-of :basic-spec/non-empty-string))
(s/def :recipe/amount pos?)
(s/def :recipe/amount-unit (union other/other-units vol/all-liquids))
(s/def :recipe/source :basic-spec/non-empty-string)

(s/def :recipe/ingredient-type #(= "ingredient" (namespace %)))
(s/def :recipe/ingredient (s/keys :req [:recipe/ingredient-type
                                        :recipe/amount
                                        :recipe/amount-unit]))
(s/def :recipe/ingredients (s/coll-of :recipe/ingredient))

(s/def :recipe/entry (s/keys :req [:recipe/name
                                   :recipe/instructions
                                   :recipe/amount
                                   :recipe/amount-unit
                                   :recipe/ingredients]
                             :opt [:recipe/source]))

(defn add-recipe [recipes new-recipe]
  (let [recipe-id (or (:recipe/id new-recipe)
                      (java.util.UUID/randomUUID))
        conformed-recipe (s/conform :recipe/entry (assoc new-recipe :recipe/id recipe-id))]
    (if (s/invalid? conformed-recipe)
      recipes
      (conj (remove #(= (:recipe/id %) recipe-id) recipes) conformed-recipe))))
