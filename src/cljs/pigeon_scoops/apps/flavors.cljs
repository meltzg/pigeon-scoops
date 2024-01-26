(ns pigeon-scoops.apps.flavors
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.instructions-dialog :refer [instructions-dialog]]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
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
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::fs/amount entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::fs/amount (js/parseFloat %))))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::fs/amount-unit entity)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::fs/amount-unit %))
             [recipe-id recipe-id-valid? on-recipe-id-change] (utils/use-validation (or (::fs/recipe-id entity)
                                                                                        (::rs/id (first mixin-recipes)))
                                                                                    #(s/valid? ::fs/recipe-id %))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace amount-unit))]
         (uix/use-effect
           (fn []
             (when (not= amount-unit-type (namespace amount-unit))
               (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                            (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))
                                            (= amount-unit-type (namespace ::ucom/pinch)) (first ucom/other-units)))))
           [amount-unit amount-unit-type on-amount-unit-change])

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Mixin")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not recipe-id-valid?)}
                     ($ InputLabel "Mixin Recipe")
                     ($ Select {:value     recipe-id
                                :on-change #(on-recipe-id-change (uuid (.. % -target -value)))}
                        (map #($ MenuItem {:value (str (::rs/id %)) :key (str (::rs/id %))} (::rs/name %)) mixin-recipes)))
                  ($ TextField {:label     "Amount"
                                :error     (not amount-valid?)
                                :value     amount
                                :on-change on-amount-change})
                  ($ FormControl {:full-width true}
                     ($ InputLabel "Amount type")
                     ($ Select {:value     amount-unit-type
                                :on-change #(set-amount-unit-type! (.. % -target -value))}
                        (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                             (map namespace [::volume/c ::mass/g]))))
                  ($ FormControl {:full-width true
                                  :error      (not amount-unit-valid?)}
                     ($ InputLabel "Amount unit")
                     ($ Select {:value     amount-unit
                                :on-change #(on-amount-unit-change (keyword amount-unit-type (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                                   (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))))))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save {::fs/recipe-id   recipe-id
                                               ::fs/amount      (js/parseFloat amount)
                                               ::fs/amount-unit amount-unit})
                          :disabled (not (and recipe-id-valid?
                                              amount-valid?
                                              amount-unit-valid?))}
                  "Save")))))

(defui flavor-entry [{:keys [entry config-metadata set-valid! set-changed-entry!]}]
       (let [flavor-id (::fs/id entry)
             set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::fs/name         ""
                                                                     ::fs/amount       0
                                                                     ::fs/amount-unit  ::volume/c
                                                                     ::fs/mixins       []
                                                                     ::fs/instructions []
                                                                     ::fs/recipe-id    nil}
                                                                    (when (some? flavor-id) [::fs/id flavor-id]))
                                                              partial-entry)))
             base-recipes (->> (:recipes config-metadata)
                               (filter #(not= (::rs/type %) ::rs/mixin))
                               (sort-by ::rs/name))
             name-valid? #(s/valid? ::fs/name (::fs/name entry))
             amount-valid? #(and (re-matches #"^\d+\.?\d*$" (str (::fs/amount entry)))
                                 (s/valid? ::fs/amount (js/parseFloat (::fs/amount entry))))
             amount-unit-valid? #(s/valid? ::fs/amount-unit (::fs/amount-unit entry))
             recipe-id-valid? #(s/valid? ::fs/recipe-id (::fs/recipe-id entry))
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace (or (::fs/amount-unit entry)
                                                                                    ::volume/c)))]
         (uix/use-effect
           (fn []
             (when (or (not (::fs/amount-unit entry)) (not= amount-unit-type (namespace (::fs/amount-unit entry))))
               (set-complete-entry!
                 (assoc entry ::fs/amount-unit
                              (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                    (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map)))))))
           [entry amount-unit-type set-complete-entry!])
         (uix/use-effect
           (fn []
             (when (nil? (::fs/recipe-id entry))
               (set-complete-entry! (assoc entry ::fs/recipe-id (::rs/id (first base-recipes))))))
           [entry base-recipes set-complete-entry!])
         (uix/use-effect
           (fn []
             (set-valid! (and (name-valid?)
                              (recipe-id-valid?)
                              (amount-valid?)
                              (amount-unit-valid?)
                              (recipe-id-valid?)))))

         ($ Stack {:direction "column"
                   :spacing   1.25}
            ($ TextField {:label     "Name"
                          :error     (not (name-valid?))
                          :value     (or (::fs/name entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::fs/name (.. % -target -value)))})
            ($ FormControl {:full-width true
                            :error      (not (recipe-id-valid?))}
               ($ InputLabel "Base Recipe")
               ($ Select {:value     (str (or (::fs/recipe-id entry)))
                          :on-change #(set-complete-entry! (assoc entry ::fs/recipe-id (uuid (.. % -target -value))))}
                  (map #($ MenuItem {:value (str (::rs/id %)) :key (str (::rs/id %))} (::rs/name %)) base-recipes)))
            ($ TextField {:label     "Amount"
                          :error     (not (amount-valid?))
                          :value     (or (::fs/amount entry) 0)
                          :on-change #(set-complete-entry! (assoc entry ::fs/amount (js/parseFloat (.. % -target -value))))})
            ($ FormControl {:full-width true}
               ($ InputLabel "Amount type")
               ($ Select {:value     amount-unit-type
                          :on-change #(set-amount-unit-type! (.. % -target -value))}
                  (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                       (map namespace [::volume/c ::mass/g]))))
            ($ FormControl {:full-width true
                            :error      (not (amount-unit-valid?))}
               ($ InputLabel "Amount unit")
               ($ Select {:value     (or (::fs/amount-unit entry)
                                         (first (keys volume/conversion-map)))
                          :on-change #(set-complete-entry! (assoc entry ::fs/amount-unit (keyword amount-unit-type (.. % -target -value))))}
                  (map #($ MenuItem {:value % :key %} (name %))
                       (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                             (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))))))
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
