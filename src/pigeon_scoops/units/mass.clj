(ns pigeon-scoops.units.mass
  (:require [clojure.set :as set-op]
            [pigeon-scoops.units.common :as common]))

(def oz-to-g 28.3495)

(def us-mass {:mass/lb (* 16 oz-to-g)
              :mass/oz oz-to-g})

(def metric-mass {:mass/kg 1000
                  :mass/g  1
                  :mass/mg (/ 1 1000)})

(def all-mass (apply set-op/union (map (comp set keys) [us-mass metric-mass])))

(def convert (partial common/convert (merge us-mass metric-mass)))
