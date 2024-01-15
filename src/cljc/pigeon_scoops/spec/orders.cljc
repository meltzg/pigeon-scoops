(ns pigeon-scoops.spec.orders
  (:require [clojure.set :refer [union]]
            [clojure.spec.alpha :as s]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.basic :as bs]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]
            [pigeon-scoops.units.common :as units]))

(s/def ::id uuid?)
(s/def ::note ::bs/non-empty-string)
(s/def ::flavor-id ::fs/id)
(s/def ::amount pos?)
(s/def ::amount-unit (union units/other-units
                            (set (keys vol/conversion-map))
                            (set (keys mass/conversion-map))))
(s/def ::flavor (s/keys :req [::flavor-id
                              ::amount
                              ::amount-unit]))

(s/def ::flavors (s/coll-of ::flavor))
(s/def ::entry (s/keys :req [::id
                             ::note
                             ::flavors]))
