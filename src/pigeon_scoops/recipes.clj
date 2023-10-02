(ns pigeon-scoops.recipes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.volume :as vol]))

(s/def :recipe/name :basic-spec/non-empty-string)
(s/def :recipe/instructions :basic-spec/non-empty-string)
(s/def :recipe/amount pos-int?)
(s/def :recipe/amount-unit vol/all-liquids)
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
