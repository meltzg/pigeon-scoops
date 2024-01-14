(ns pigeon-scoops.spec.flavors
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.basic :as bs]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]
            [pigeon-scoops.units.common :as units]))

(s/def ::id uuid?)
(s/def ::name ::bs/non-empty-string)
(s/def ::instructions (s/coll-of ::bs/non-empty-string))
(s/def ::recipe-id ::rs/id)
(s/def ::amount pos?)
(s/def ::amount-unit (union units/other-units
                            (set (keys vol/conversion-map))
                            (set (keys mass/conversion-map))))
(s/def ::mixin (s/keys :req [::recipe-id
                             ::amount
                             ::amount-unit]))

(s/def ::mixins (s/coll-of ::mixin))
(s/def ::entry (s/keys :req [::id
                             ::name
                             ::instructions
                             ::recipe-id
                             ::amount
                             ::amount-unit
                             ::mixins]))
