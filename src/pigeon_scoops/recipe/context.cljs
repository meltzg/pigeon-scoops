(ns pigeon-scoops.recipe.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def recipes-context (uix/create-context))
(def recipe-context (uix/create-context))

(defui with-recipes [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-recipes token)
                   (.then vals)
                   (.then (partial apply concat))
                   (.then set-recipes!))))
           [token refresh?])
         ($ (.-Provider recipes-context) {:value {:recipes     recipes
                                                  :new-recipe! #(do
                                                                  (set-recipes! (conj recipes {:recipe/id :new}))
                                                                  :new)
                                                  :refresh!    #(set-refresh! (not refresh?))}}
            children)))

(defui with-recipe [{:keys [recipe-id scaled-amount scaled-amount-unit children]}]
       (let [{:keys [token]} (use-token)
             [recipe set-recipe!] (uix/use-state nil)
             [editable-recipe set-editable-recipe!] (uix/use-state nil)
             unsaved-changes? (not= recipe editable-recipe)
             set-ingredient! #(set-editable-recipe! (update editable-recipe
                                                            :recipe/ingredients
                                                            (fn [ingredients]
                                                              (map (fn [i]
                                                                     (if (= (:ingredient/id i)
                                                                            (:ingredient/id %)) % i))
                                                                   ingredients))))
             remove-ingredient! (fn [ingredient-id]
                                  (set-editable-recipe! (update editable-recipe
                                                                :recipe/ingredients
                                                                (partial
                                                                  remove
                                                                  #(= ingredient-id (:ingredient/id %))))))
             new-ingredient! (fn []
                               (set-editable-recipe! (update editable-recipe
                                                             :recipe/ingredients
                                                             #(conj % {:ingredient/id :new}))))
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (cond (keyword? recipe-id)
                   ((juxt set-recipe! set-editable-recipe!) {})
                   (and recipe-id token)
                   (.then (api/get-recipe token recipe-id (if (some? scaled-amount)
                                                            {:amount      scaled-amount
                                                             :amount-unit scaled-amount-unit}
                                                            {}))
                          (juxt set-recipe! set-editable-recipe!))))
           [refresh? token recipe-id scaled-amount scaled-amount-unit])
         ($ (.-Provider recipe-context) {:value {:recipe               recipe
                                                 :editable-recipe      editable-recipe
                                                 :set-editable-recipe! set-editable-recipe!
                                                 :set-ingredient!      set-ingredient!
                                                 :remove-ingredient!   remove-ingredient!
                                                 :new-ingredient!      new-ingredient!
                                                 :unsaved-changes?     unsaved-changes?
                                                 :refresh!             #(set-refresh! (not refresh?))}}
            children)))