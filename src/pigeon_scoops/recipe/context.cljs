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
               (.then (api/get-recipes token) set-recipes!)))
           [token refresh?])
         ($ (.-Provider recipes-context) {:value {:recipes  (apply concat (vals recipes))
                                                  :refresh! #(set-refresh! (not refresh?))}}
            children)))

(defui with-recipe [{:keys [recipe-id scaled-amount scaled-amount-unit children]}]
       (let [{:keys [token]} (use-token)
             [recipe set-recipe!] (uix/use-state nil)
             [recipe-name set-name!] (uix/use-state (or (:recipe/name recipe) ""))
             [public set-public!] (uix/use-state (or (:recipe/public recipe) false))
             [amount set-amount!] (uix/use-state (:recipe/amount recipe))
             [amount-unit set-amount-unit!] (uix/use-state (or (:recipe/amount-unit recipe) ""))
             [source set-source!] (uix/use-state (or (:recipe/source recipe) ""))
             [instructions set-instructions!] (uix/use-state (or (:recipe/instructions recipe) ""))
             [ingredients set-ingredients!] (uix/use-state (:recipe/ingredients recipe))


             unsaved-changes? (not-every? true? (map #(= ((first %) recipe) (second %)) {:recipe/name         recipe-name
                                                                                         :recipe/public       public
                                                                                         :recipe/amount       amount
                                                                                         :recipe/amount-unit  amount-unit
                                                                                         :recipe/source       source
                                                                                         :recipe/instructions instructions
                                                                                         :recipe/ingredients  ingredients}))
             set-ingredient! #(set-ingredients! (map (fn [i]
                                                       (if (= (:ingredient/id i)
                                                              (:ingredient/id %)) % i))
                                                     ingredients))
             remove-ingredient! (fn [ingredient-id]
                                  (set-ingredients! (remove #(= ingredient-id (:ingredient/id %))
                                                            ingredients)))
             reset! (uix/use-memo #(fn [r]
                                     (set-name! (or (:recipe/name r) ""))
                                     (set-public! (or (:recipe/public r) false))
                                     (set-amount! (:recipe/amount r))
                                     (set-amount-unit! (:recipe/amount-unit r))
                                     (set-source! (or (:recipe/source r) ""))
                                     (set-instructions! (or (:recipe/instructions r) ""))
                                     (set-ingredients! (:recipe/ingredients r)))
                                  [])
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when (and recipe-id token)
               (.then (api/get-recipe token recipe-id (if (some? scaled-amount)
                                                        {:amount      scaled-amount
                                                         :amount-unit scaled-amount-unit}
                                                        {}))
                      (juxt set-recipe! reset!))))
           [reset! refresh? token recipe-id scaled-amount scaled-amount-unit])
         ($ (.-Provider recipe-context) {:value {:recipe             recipe
                                                 :recipe-name        recipe-name
                                                 :set-name!          set-name!
                                                 :public             public
                                                 :set-public!        set-public!
                                                 :scaled-amount      scaled-amount
                                                 :scaled-amount-unit scaled-amount-unit
                                                 :amount             amount
                                                 :set-amount!        set-amount!
                                                 :amount-unit        amount-unit
                                                 :set-amount-unit!   set-amount-unit!
                                                 :source             source
                                                 :set-source!        set-source!
                                                 :instructions       instructions
                                                 :set-instructions!  set-instructions!
                                                 :ingredients        ingredients
                                                 :set-ingredient!    set-ingredient!
                                                 :remove-ingredient! remove-ingredient!
                                                 :unsaved-changes?   unsaved-changes?
                                                 :reset!             reset!
                                                 :refresh!           #(set-refresh! (not refresh?))}}
            children)))