(ns pigeon-scoops.units.common
  (:require [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as vol]))

(def other-units #{:common/pinch
                   :common/unit})

(defn convert [val from to]
  (let [conversion-map (cond
                         (some #{from} (keys mass/conversion-map))
                         mass/conversion-map
                         (some #{from} (keys vol/conversion-map))
                         vol/conversion-map)]
    (if (not (and (from conversion-map)
                  (to conversion-map)))
      nil
      (let [standard-mass (* val (from conversion-map))
            conversion-factor (to conversion-map)]
        (/ standard-mass conversion-factor)))))

(defn scale-factor [amount-from unit-from amount-to unit-to]
  (when-let [conversion-factor (convert 1 unit-to unit-from)]
    (* amount-from amount-to conversion-factor)))
