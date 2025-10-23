(ns pigeon-scoops.menu.views
  (:require [uix.core :as uix :refer [$ defui]]
            ["@mui/material" :refer [Button
                                     Card
                                     CardActions
                                     CardContent
                                     Stack]]))

(defui menu-card [order])

(defui menu-view []
       ($ Stack {:direction "row"}
          ($ Button "New Menu")))
