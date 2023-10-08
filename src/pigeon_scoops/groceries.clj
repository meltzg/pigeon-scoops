(ns pigeon-scoops.groceries
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops.basic-spec]
            [pigeon-scoops.units.common :as common]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(s/def :grocery/type #(= "grocery" (namespace %)))
(s/def :grocery/description :basic-spec/non-empty-string)

(s/def :grocery/source :basic-spec/non-empty-string)
(s/def :grocery/unit-volume pos?)
(s/def :grocery/unit-volume-type (set (keys vol/conversion-map)))
(s/def :grocery/unit-mass pos?)
(s/def :grocery/unit-mass-type (set (keys mass/conversion-map)))
(s/def :grocery/unit-common pos?)
(s/def :grocery/unit-common-type common/other-units)
(s/def :grocery/unit-cost pos?)

(s/def :grocery/unit (s/keys :req [:grocery/source
                                   :grocery/unit-cost]
                             :opt [:grocery/unit-mass
                                   :grocery/unit-mass-type
                                   :grocery/unit-volume
                                   :grocery/unit-volume-type
                                   :grocery/unit-common
                                   :grocery/unit-common-type]))
(s/def :grocery/units (s/coll-of :grocery/unit))

(s/def :grocery/entry (s/keys :req [:grocery/type
                                    :grocery/units]
                              :opt [:grocery/description]))

(defn add-grocery-item [groceries new-grocery-item]
  (let [conformed-ingredient (s/conform :grocery/entry new-grocery-item)]
    (if (s/invalid? conformed-ingredient)
      groceries
      (conj (remove #(= (:grocery/type %) (:grocery/type new-grocery-item)) groceries) conformed-ingredient))))
