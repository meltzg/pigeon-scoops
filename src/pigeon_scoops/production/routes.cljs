(ns pigeon-scoops.production.routes
  (:require
   [pigeon-scoops.production.views :refer [production-view]]))

(def routes ["/production"
             ["" {:name ::production
                  :view production-view}]])
