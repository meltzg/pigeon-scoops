(ns pigeon-scoops.units.common)

(def other-units #{:common/pinch
                   :common/unit})

(defn convert [conversion-map val from to]
  (if (not (and (from conversion-map)
                (to conversion-map)))
    nil
    (let [standard-mass (* val (from conversion-map))
          conversion-factor (to conversion-map)]
      (/ standard-mass conversion-factor))))
