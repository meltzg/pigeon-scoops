(ns pigeon-scoops.forms.flavors
  (:require [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.components.amount-config :refer [amount-config]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.instructions-dialog :refer [instructions-dialog]]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
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

(defui mixin-config [{:keys [entity config-metadata on-save on-close]}]
       (let [{:keys [recipes]} config-metadata
             mixin-recipes (->> recipes
                                (filter #(= (::rs/type %) ::rs/mixin))
                                (sort-by ::rs/name))
             [entity set-entity!] (uix/use-state entity)
             set-complete-entity! (fn [partial-entity]
                                    (set-entity! (merge {::fs/recipe-id   nil
                                                         ::fs/amount      0
                                                         ::fs/amount-unit nil}
                                                        partial-entity)))
             [amount-config-valid? set-amount-config-valid!] (uix/use-state false)
             recipe-id-valid? #(and (::fs/recipe-id entity)
                                    (s/valid? ::fs/recipe-id (::fs/recipe-id entity)))]
         (uix/use-effect
           (fn []
             (set-complete-entity! (assoc entity ::fs/recipe-id (or (::fs/recipe-id entity)
                                                                    (::rs/id (first mixin-recipes)))))))

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Mixin")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not (recipe-id-valid?))}
                     ($ InputLabel "Mixin Recipe")
                     ($ Select {:value     (or (::fs/recipe-id entity)
                                               (::rs/id (first mixin-recipes)))
                                :on-change #(set-complete-entity!
                                              (assoc entity ::fs/recipe-id (uuid (.. % -target -value))))}
                        (map #($ MenuItem {:value (str (::rs/id %)) :key (str (::rs/id %))} (::rs/name %)) mixin-recipes)))
                  ($ amount-config {:entry               entity
                                    :on-change           set-complete-entity!
                                    :set-valid!          set-amount-config-valid!
                                    :entry-namespace     (namespace ::fs/id)
                                    :default-amount-unit ::volume/c
                                    :accepted-unit-types [::volume/c ::mass/g]})))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save entity)
                          :disabled (not (and (recipe-id-valid?)
                                              amount-config-valid?))}
                  "Save")))))

(defui flavor-entry [{:keys [entry config-metadata set-valid! set-changed-entry!]}]
       (let [flavor-id (::fs/id entry)
             set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::fs/name         ""
                                                                     ::fs/amount       0
                                                                     ::fs/amount-unit  nil
                                                                     ::fs/mixins       []
                                                                     ::fs/instructions []
                                                                     ::fs/recipe-id    nil}
                                                                    (when (some? flavor-id) [::fs/id flavor-id]))
                                                              partial-entry)))
             base-recipes (->> (:recipes config-metadata)
                               (filter #(not= (::rs/type %) ::rs/mixin))
                               (sort-by ::rs/name))
             name-valid? #(s/valid? ::fs/name (::fs/name entry))
             [amount-config-valid? set-amount-config-valid!] (uix/use-state false)
             recipe-id-valid? #(s/valid? ::fs/recipe-id (::fs/recipe-id entry))
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)]
         (uix/use-effect
           (fn []
             (set-valid! (and (name-valid?)
                              (recipe-id-valid?)
                              amount-config-valid?
                              (recipe-id-valid?)))))
         (uix/use-effect
           (fn []
             (when (nil? (::fs/recipe-id entry))
               (set-complete-entry! (assoc entry ::fs/recipe-id (::rs/id (first base-recipes))))))
           [entry base-recipes set-complete-entry!])

         ($ Stack {:direction "column"
                   :spacing   1.25}
            ($ TextField {:label     "Name"
                          :error     (not (name-valid?))
                          :value     (or (::fs/name entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::fs/name (.. % -target -value)))})
            ($ FormControl {:full-width true
                            :error      (not (recipe-id-valid?))}
               ($ InputLabel "Base Recipe")
               ($ Select {:value     (str (::fs/recipe-id entry))
                          :on-change #(set-complete-entry! (assoc entry ::fs/recipe-id (uuid (.. % -target -value))))}
                  (map #($ MenuItem {:value (str (::rs/id %)) :key (str (::rs/id %))} (::rs/name %)) base-recipes)))
            ($ amount-config {:entry               entry
                              :on-change           set-complete-entry!
                              :set-valid!          set-amount-config-valid!
                              :entry-namespace     (namespace ::fs/id)
                              :default-amount-unit ::volume/c
                              :accepted-unit-types [::volume/c ::mass/g]})
            ($ Typography "Mixins")
            ($ entity-list {:entity-name     "Mixins"
                            :entities        (::fs/mixins entry)
                            :column-headers  ["Type"
                                              "Amount"]
                            :cell-text       (for [mixin (::fs/mixins entry)]
                                               [(::rs/name (first (filter #(= (::rs/id %)
                                                                              (::fs/recipe-id mixin))
                                                                          (:recipes config-metadata))))
                                                (str (::fs/amount mixin)
                                                     " "
                                                     (name (::fs/amount-unit mixin)))])
                            :config-metadata config-metadata
                            :entity-config   mixin-config
                            :on-change       #(set-complete-entry! (assoc entry ::fs/mixins %))})
            (when edit-instructions-open
              ($ instructions-dialog {:instructions (::fs/instructions entry)
                                      :validate-fn  #(s/valid? ::rs/instructions %)
                                      :on-close     #(set-edit-instructions-open! false)
                                      :on-save      #(do (set-complete-entry! (assoc entry ::fs/instructions %))
                                                         (set-edit-instructions-open! false))}))
            ($ Typography
               "Instructions")
            ($ Paper
               ($ List
                  (map-indexed (fn [idx, text]
                                 ($ ListItem {:key (str text idx)}
                                    ($ ListItemText {:primary (str (inc idx) ") " text)})))
                               (::fs/instructions entry))))
            ($ Button {:variant  "contained"
                       :on-click #(set-edit-instructions-open! true)}
               "Edit Instructions"))))
