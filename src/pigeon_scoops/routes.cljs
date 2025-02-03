(ns pigeon-scoops.routes
  (:require [pigeon-scoops.groceries :refer [grocery-view groceries-table]]
            [uix.core :refer [$ defui]]))

(defui item [props]
       ($ :div (str (js->clj props :keywordize-keys true))))

(def routes
  [["/" {:name ::root
         :view item}]
   ["/grocery"
    ["" {:name ::groceries
         :view groceries-table}]
    ["/:grocery-id" {:name       ::grocery
                     :view       grocery-view
                     :parameters {:path {:grocery-id uuid?}}}]]
   ["/recipe" {:name ::recipe
               :view item}]
   ["/order" {:name ::order
              :view item}]])
