(ns pigeon-scoops.routes
  (:require [pigeon-scoops.groceries :refer [grocery-view]]
            [uix.core :refer [$ defui]]))

(defui item [props]
       ($ :div (str (js->clj props :keywordize-keys true))))

(def routes
  [["/" {:name ::root
         :view item}]
   ["/grocery"
    ["" {:name ::groceries
         :view grocery-view}]
    ["/:grocery-id" {:name       ::grocery
                     :view       grocery-view
                     :parameters {:path {:grocery-id uuid?}}}]]
   ["/recipe" {:name ::recipe
               :view item}]
   ["/order" {:name ::order
              :view item}]])
