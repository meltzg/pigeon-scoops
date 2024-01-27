(ns pigeon-scoops.forms.recipes
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.amount-config :refer [amount-config]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.instructions-dialog :refer [instructions-dialog]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            ["@mui/material" :refer [Button
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
             [ingredient-type ingredient-type-valid? on-ingredient-type-change] (utils/use-validation (or (::rs/ingredient-type entity)
                                                                                                          (::gs/type (first groceries)))
                                                                                                      #(s/valid? ::rs/ingredient-type %))
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::rs/amount entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::rs/amount (js/parseFloat %))))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace (or (::rs/amount-unit entity) ::volume/c)))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::rs/amount-unit entity)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::rs/amount-unit %))]

         (uix/use-effect
           (fn []
             (when (not= amount-unit-type (namespace amount-unit))
               (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                            (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))
                                            (= amount-unit-type (namespace ::ucom/pinch)) (first ucom/other-units)))))
           [amount-unit amount-unit-type on-amount-unit-change])

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Ingredient")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not ingredient-type-valid?)}
                     ($ InputLabel "Type")
                     ($ Select {:value     ingredient-type
                                :on-change #(on-ingredient-type-change (keyword (namespace ::gs/type) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (sort (map ::gs/type groceries)))))
                  ($ TextField {:label     "Amount"
                                :error     (not amount-valid?)
                                :value     amount
                                :on-change on-amount-change})
                  ($ FormControl {:full-width true}
                     ($ InputLabel "Amount type")
                     ($ Select {:value     amount-unit-type
                                :on-change #(set-amount-unit-type! (.. % -target -value))}
                        (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                             (map namespace [::volume/c ::mass/g ::ucom/pinch]))))
                  ($ FormControl {:full-width true
                                  :error      (not amount-unit-valid?)}
                     ($ InputLabel "Amount unit")
                     ($ Select {:value     amount-unit
                                :on-change #(on-amount-unit-change (keyword amount-unit-type (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                                   (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))
                                   (= amount-unit-type (namespace ::ucom/pinch)) ucom/other-units))))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save {::rs/ingredient-type ingredient-type
                                               ::rs/amount          (js/parseFloat amount)
                                               ::rs/amount-unit     amount-unit})
                          :disabled (not (and ingredient-type-valid?
                                              amount-valid?
                                              amount-unit-valid?))}
                  "Save")))))

(defui recipe-entry [{:keys [entry config-metadata set-valid! set-changed-entry!]}]
       (let [recipe-id (::rs/id entry)
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
                          :error     (not (name-valid?))
                          :value     (or (::rs/name entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::rs/name (.. % -target -value)))})
            ($ FormControl {:full-width true
                            :error      (not (type-valid?))}
               ($ InputLabel "Recipe type")
               ($ Select {:value     (::rs/type entry)
                          :on-change #(set-complete-entry!
                                        (assoc entry ::rs/type
                                                     (keyword (namespace ::rs/type) (.. % -target -value))))}
                  (map #($ MenuItem {:value % :key %} (name %)) (sort rs/recipe-types))))
            ($ amount-config {:entry               entry
                              :on-change           set-complete-entry!
                              :set-valid!          set-amount-config-valid!
                              :entry-namespace     (namespace ::rs/id)
                              :default-amount-unit ::volume/c
                              :accepted-unit-types [::volume/c ::mass/g]})
            ($ TextField {:label     "Source"
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
                            :on-change       #(set-complete-entry! (assoc entry ::rs/ingredients %))})
            ($ Typography
               "Instructions")
            ($ Paper
               ($ List
                  (map-indexed (fn [idx, text]
                                 ($ ListItem {:key text}
                                    ($ ListItemText {:primary (str (inc idx) ") " text)})))
                               (::rs/instructions entry))))
            ($ Button {:variant  "contained"
                       :on-click #(set-edit-instructions-open! true)}
               "Edit Instructions"))))
