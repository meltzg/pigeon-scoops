(ns pigeon-scoops.spec.groceries
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops.spec.basic :as bs]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(s/def ::type #(= (namespace ::g) (namespace %)))
(s/def ::description ::bs/non-empty-string)

(s/def ::source ::bs/non-empty-string)
(s/def ::unit-volume pos?)
(s/def ::unit-volume-type (set (keys vol/conversion-map)))
(s/def ::unit-mass pos?)
(s/def ::unit-mass-type (set (keys mass/conversion-map)))
(s/def ::unit-common pos?)
(s/def ::unit-common-type units/other-units)
(s/def ::unit-cost pos?)
(s/def ::unit-purchase-quantity pos-int?)

(s/def ::unit (s/keys :req [::source
                            ::unit-cost]
                      :opt [::unit-mass
                            ::unit-mass-type
                            ::unit-volume
                            ::unit-volume-type
                            ::unit-common
                            ::unit-common-type
                            ::unit-purchase-quantity]))
(s/def ::units (s/coll-of ::unit))

(s/def ::entry (s/keys :req [::type
                             ::units]
                       :opt [::description
                             ::amount-needed
                             ::amount-needed-unit]))
