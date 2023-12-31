(ns pigeon-scoops.spec.recipes
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.spec.basic :as bs]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]
            [pigeon-scoops.units.common :as units]))

(def recipe-types #{::ice-cream ::sorbet ::mixin})

(s/def ::id uuid?)
(s/def ::type recipe-types)
(s/def ::name ::bs/non-empty-string)
(s/def ::instructions (s/coll-of ::bs/non-empty-string))
(s/def ::amount pos?)
(s/def ::amount-unit (union units/other-units
                            (set (keys vol/conversion-map))
                            (set (keys mass/conversion-map))))
(s/def ::source ::bs/non-empty-string)

(s/def ::ingredient-type ::gs/type)
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
