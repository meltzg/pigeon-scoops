(ns pigeon-scoops.recipe.routes
  (:require [pigeon-scoops.recipe.views :refer [recipe-view recipes-table]]
            [spec-tools.data-spec :as ds]))

(def routes
  ["/recipe"
   ["" {:name ::recipes
        :view recipes-table}]
   ["/:recipe-id" {:name       ::recipe
                   :view       recipe-view
                   :parameters {:path  {:recipe-id uuid?}
                                :query {(ds/opt :amount)      number?
                                        (ds/opt :amount-unit) keyword?}}}]])
