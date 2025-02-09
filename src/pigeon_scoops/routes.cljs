(ns pigeon-scoops.routes
  (:require [cljs.spec.alpha :as s]
            [pigeon-scoops.groceries :refer [groceries-table grocery-view]]
            [pigeon-scoops.recipes :refer [recipe-view recipes-table]]
            [spec-tools.data-spec :as ds]
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
                     :parameters {:path {:grocery-id (s/or :uuid uuid? :key keyword?)}}}]]
   ["/recipe"
    ["" {:name ::recipes
         :view recipes-table}]
    ["/:recipe-id" {:name       ::recipe
                    :view       recipe-view
                    :parameters {:path  {:recipe-id uuid?}
                                 :query {(ds/opt :amount)      number?
                                         (ds/opt :amount-unit) keyword?}}}]]
   ["/order" {:name ::order
              :view item}]])
