(ns pigeon-scoops.routes
  (:require [pigeon-scoops.grocery.routes :as grocery]
            [pigeon-scoops.recipe.routes :as recipe]
            [uix.core :refer [$ defui]]))

(defui item [props]
       ($ :div (str (js->clj props :keywordize-keys true))))

(def routes
  [["/" {:name ::root
         :view item}]
   grocery/routes
   recipe/routes
   ["/order" {:name ::order
              :view item}]])
