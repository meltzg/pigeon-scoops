(ns pigeon-scoops.forms.recipes
  (:require [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.components.amount-config :refer [amount-config]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.instructions-dialog :refer [instructions-dialog]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            ["@mui/material" :refer [Backdrop
                                     Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     FormControl
                                     InputLabel
                                     List
                                     ListItem
                                     ListItemText
                                     MenuItem
                                     Paper
                                     Select
                                     Stack
                                     TextField
                                     Typography]]))

(defui ingredient-config [{:keys [entity config-metadata on-save on-close]}]
       (let [{:keys [groceries]} config-metadata
             [entity set-entity!] (uix/use-state entity)
             set-complete-entity! (fn [partial-entity]
                                    (set-entity! (merge {::rs/ingredient-type nil
                                                         ::rs/amount          0
                                                         ::rs/amount-unit     nil}
                                                        partial-entity)))
             [amount-config-valid? set-amount-config-valid!] (uix/use-state false)
             ingredient-type-valid? #(and (::rs/ingredient-type entity)
                                          (s/valid? ::rs/ingredient-type (::rs/ingredient-type entity)))]
         (uix/use-effect
           (fn []
             (set-complete-entity! (assoc entity ::rs/ingredient-type (or (::rs/ingredient-type entity)
                                                                          (::gs/type (first groceries)))))))

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Ingredient")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not (ingredient-type-valid?))}
                     ($ InputLabel "Type")
                     ($ Select {:value     (or (::rs/ingredient-type entity)
                                               (::gs/type (first groceries)))
                                :on-change #(set-complete-entity!
                                              (assoc entity ::rs/ingredient-type
                                                            (keyword (namespace ::gs/type) (.. % -target -value))))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (sort (map ::gs/type groceries)))))
                  ($ amount-config {:entry               entity
                                    :on-change           set-complete-entity!
                                    :set-valid!          set-amount-config-valid!
                                    :entry-namespace     (namespace ::rs/id)
                                    :default-amount-unit ::volume/c
                                    :accepted-unit-types [::volume/c ::mass/g, ::ucom/pinch]})))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save entity)
                          :disabled (not (and (ingredient-type-valid?)
                                              amount-config-valid?))}
                  "Save")))))

(defui recipe-entry [{:keys [entry config-metadata set-valid! set-changed-entry!]}]
       (let [recipe-id (::rs/id entry)
             frozen? (nil? set-changed-entry!)
             set-valid! (or set-valid! identity)
             set-changed-entry! (or set-changed-entry! identity)
             set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::rs/name         ""
                                                                     ::rs/type         nil
                                                                     ::rs/amount       0
                                                                     ::rs/amount-unit  nil
                                                                     ::rs/ingredients  []
                                                                     ::rs/instructions []}
                                                                    (when (some? recipe-id) [::rs/id recipe-id]))
                                                              partial-entry)))
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)
             name-valid? #(s/valid? ::rs/name (::rs/name entry))
             type-valid? #(s/valid? ::rs/type (::rs/type entry))
             [amount-config-valid? set-amount-config-valid!] (uix/use-state false)]

         (uix/use-effect
           (fn []
             (set-valid! (and (name-valid?)
                              (type-valid?)
                              amount-config-valid?))))
         (uix/use-effect
           (fn []
             (when (nil? (::rs/type entry))
               (set-complete-entry! (assoc entry ::rs/type (first rs/recipe-types)))))
           [entry set-complete-entry!])

         ($ Stack {:direction "column"
                   :spacing   1.25}
            ($ TextField {:label     "Name"
                          :disabled  frozen?
                          :error     (not (name-valid?))
                          :value     (or (::rs/name entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::rs/name (.. % -target -value)))})
            ($ FormControl {:full-width true
                            :error      (not (type-valid?))}
               ($ InputLabel "Recipe type")
               ($ Select {:value     (::rs/type entry)
                          :disabled  frozen?
                          :on-change #(set-complete-entry!
                                        (assoc entry ::rs/type
                                                     (keyword (namespace ::rs/type) (.. % -target -value))))}
                  (map #($ MenuItem {:value % :key %} (name %)) (sort rs/recipe-types))))
            ($ amount-config {:entry               entry
                              :on-change           set-complete-entry!
                              :set-valid!          set-amount-config-valid!
                              :entry-namespace     (namespace ::rs/id)
                              :default-amount-unit ::volume/c
                              :accepted-unit-types [::volume/c ::mass/g]
                              :frozen?             frozen?})
            ($ TextField {:label     "Source"
                          :disabled  frozen?
                          :value     (or (::rs/source entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::rs/source (.. % -target -value)))})
            (when edit-instructions-open
              ($ instructions-dialog {:instructions (::rs/instructions entry)
                                      :validate-fn  #(s/valid? ::rs/instructions %)
                                      :on-close     #(set-edit-instructions-open! false)
                                      :on-save      #(do (set-complete-entry! (assoc entry ::rs/instructions %))
                                                         (set-edit-instructions-open! false))}))
            ($ entity-list {:entity-name     "Ingredient"
                            :entities        (::rs/ingredients entry)
                            :column-headers  ["Type"
                                              "Amount"]
                            :cell-text       (for [ingredient (::rs/ingredients entry)]
                                               [(name (::rs/ingredient-type ingredient))
                                                (str (::rs/amount ingredient)
                                                     " "
                                                     (name (::rs/amount-unit ingredient)))])
                            :config-metadata {:groceries (:groceries config-metadata)}
                            :entity-config   ingredient-config
                            :frozen?         frozen?
                            :on-change       #(set-complete-entry! (assoc entry ::rs/ingredients %))})
            ($ Typography
               "Instructions")
            ($ Paper
               ($ List
                  (map-indexed (fn [idx, text]
                                 ($ ListItem {:key text}
                                    ($ ListItemText {:primary (str (inc idx) ") " text)})))
                               (::rs/instructions entry))))
            (when-not frozen?
              ($ Button {:variant  "contained"
                         :on-click #(set-edit-instructions-open! true)}
                 "Edit Instructions")))))
