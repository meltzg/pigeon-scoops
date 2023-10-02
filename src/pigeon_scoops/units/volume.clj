(ns pigeon-scoops.units.volume
  (:require [clojure.set :as set-op]))

(def us-liquid #{:volume/gallon
                 :volume/quart
                 :volume/pint
                 :volume/cup
                 :volume/floz
                 :volume/tbsp
                 :volume/tsp})

(def metric-liquid #{:volume/liter
                     :volume/deciliter
                     :volume/centiliter
                     :volume/milliliter})

(def all-liquids (set-op/union us-liquid metric-liquid))
