(ns pigeon-scoops.menu.routes
  (:require [pigeon-scoops.menu.views :refer [menu-view]]))

(def routes
  ["/menu" {:name ::menu
            :view menu-view}])
