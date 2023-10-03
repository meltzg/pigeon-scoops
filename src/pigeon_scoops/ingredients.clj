(ns pigeon-scoops.ingredients
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(s/def :ingredient/type #(= "ingredient" (namespace %)))
(s/def :ingredient/description :basic-spec/non-empty-string)

(s/def :ingredient/source :basic-spec/non-empty-string)
(s/def :ingredient/unit-volume pos?)
(s/def :ingredient/unit-volume-type vol/all-liquids)
(s/def :ingredient/unit-mass pos?)
(s/def :ingredient/unit-mass-type mass/all-mass)
(s/def :ingredient/unit-cost pos?)

(s/def :ingredient/unit (s/keys :req [:ingredient/source
                                      :ingredient/unit-mass
                                      :ingredient/unit-mass-type
                                      :ingredient/unit-volume
                                      :ingredient/unit-volume-type
                                      :ingredient/unit-cost]))
(s/def :ingredient/units (s/coll-of :ingredient/unit))

(s/def :ingredient/entry (s/keys :req [:ingredient/type
                                       :ingredient/description
                                       :ingredient/units]))

(defn add-ingredient [ingredients new-ingredient]
  (let [conformed-ingredient (s/conform :ingredient/entry new-ingredient)]
    (if (s/invalid? conformed-ingredient)
      ingredients
      (conj (remove #(= (:ingredient/type %) (:ingredient/type new-ingredient)) ingredients) conformed-ingredient))))
