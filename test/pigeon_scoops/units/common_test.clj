(ns pigeon-scoops.units.common-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.common-test :refer [tolerance]]))

(deftest convert-test
  (testing "can convert units correctly and can be converted back"
    (are [val from to expected] (and (< (abs (- expected (units/convert val from to)))
                                        tolerance)
                                     (< (abs (- val (units/convert (units/convert val from to) to from)))
                                        tolerance))
                                ;; mass conversion
                                1 :mass/lb :mass/oz 16
                                2 :mass/kg :mass/lb 4.40925
                                3 :mass/kg :mass/g 3000
                                4000 :mass/mg :mass/g 4
                                ;; volume conversion
                                1 :volume/gal :volume/qt 4
                                1 :volume/qt :volume/pt 2
                                1 :volume/pt :volume/c 2
                                1 :volume/c :volume/floz 8
                                1 :volume/floz :volume/tbsp 2
                                1 :volume/tbsp :volume/tsp 3
                                768 :volume/tsp :volume/gal 1
                                1 :volume/l :volume/ml 1000
                                1 :volume/l :volume/gal 0.2642)))

(deftest convert-invalid-test
  (testing "invalid conversions return nil"
    (are [from to] (nil? (units/convert 8 from to))
                   :mass/oz :other/pinch
                   :other/pinch :mass/oz
                   :volume/tbsp :other/pinch
                   :other/pinch :volume/tbsp
                   :volume/c :mass/g
                   :mass/kg :volume/floz)))