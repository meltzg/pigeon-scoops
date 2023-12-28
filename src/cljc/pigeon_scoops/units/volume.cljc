(ns pigeon-scoops.units.volume)

(def floz-to-ml 29.5735)

(def us-liquid {::gal  (* 128 floz-to-ml)
                ::qt   (* 32 floz-to-ml)
                ::pt   (* 16 floz-to-ml)
                ::c    (* 8 floz-to-ml)
                ::floz floz-to-ml
                ::tbsp (/ floz-to-ml 2)
                ::tsp  (/ floz-to-ml 6)})

(def metric-liquid {::l  1000
                    ::ml 1})

(def conversion-map (merge us-liquid metric-liquid))
