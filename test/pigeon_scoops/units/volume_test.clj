(ns pigeon-scoops.units.volume-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops.units.volume :as vol]
            [pigeon-scoops.common-test :refer [tolerance]]))

(deftest convert-test
  (testing "can convert units correctly and can be converted back"
    (are [val from to expected] (and (< (abs (- expected (vol/convert val from to)))
                                        tolerance)
                                     (< (abs (- val (vol/convert (vol/convert val from to) to from)))
                                        tolerance))
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
    (are [from to] (nil? (vol/convert 8 from to))
                   :volume/tbsp :other/pinch
                   :other/pinch :volume/tbsp)))
