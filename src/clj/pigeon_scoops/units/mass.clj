(ns pigeon-scoops.units.mass)

(def oz-to-g 28.3495)

(def us-mass {::lb (* 16 oz-to-g)
              ::oz oz-to-g})

(def metric-mass {::kg 1000
                  ::g  1
                  ::mg (/ 1 1000)})

(def conversion-map (merge us-mass metric-mass))
