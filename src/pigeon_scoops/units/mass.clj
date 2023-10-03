(ns pigeon-scoops.units.mass
  (:require [clojure.set :as set-op]))

(def us-mass #{:mass/lb
               :mass/oz})

(def metric-mass #{:mass/kilogram
                   :mass/gram
                   :mass/milligram})

(def all-mass (set-op/union us-mass metric-mass))
