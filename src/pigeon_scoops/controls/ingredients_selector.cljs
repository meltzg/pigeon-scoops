(ns pigeon-scoops.controls.ingredients-selector
  (:require [uix.core :as uix :refer [$ defui]]
            [antd :refer [Select Spin]]
            [pigeon-scoops.hooks :refer [use-groceries use-recipes]]))

(defui ingredients-selector [{:keys [value on-change]}]
  (let [{:keys [groceries] groceries-loading? :loading?} (use-groceries)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        recipes (apply concat (vals recipes))
        [options set-options!] (uix/use-state [])]
    (uix/use-effect
     (fn []
       (when-not (or groceries-loading? recipes-loading?)
         (set-options! (sort-by :label (concat
                                        (for [g groceries]
                                          {:value (str "grocery:" (:grocery/id g))
                                           :label (:grocery/name g)})
                                        (for [r recipes]
                                          {:value (str "recipe:" (:recipe/id r))
                                           :label (:recipe/name r)}))))))
     [groceries recipes groceries-loading? recipes-loading?])
    (if (or groceries-loading? recipes-loading?)
      ($ Spin)
      ($ Select {:value value
                 :show-search (clj->js {:optionFilterProp :label})
                 :on-change #(on-change (update (into {} (map vector [:type :id] (.split % ":")))
                                                :type keyword))
                 :options (clj->js options)}))))
