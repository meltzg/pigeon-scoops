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
            ["@mui/material" :refer [Accordion
                                     AccordionDetails
                                     AccordionSummary
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

(defui flavor-entry [{:keys [flavor recipes on-save on-delete]}]
       (let [flavor-id (::fs/id flavor)
             base-recipes (->> recipes
                               (filter #(not= (::rs/type %) ::rs/mixin))
                               (sort-by ::rs/name))
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)
             [flavor-name flavor-name-valid? on-flavor-name-change] (utils/use-validation (or (::fs/name flavor) "")
                                                                                          #(s/valid? ::fs/name %))
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::fs/amount flavor) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::fs/amount (js/parseFloat %))))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::fs/amount-unit flavor)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::fs/amount-unit %))
             [recipe-id recipe-id-valid? on-recipe-id-change] (utils/use-validation (or (::fs/recipe-id flavor)
                                                                                        (::rs/id (first base-recipes)))
                                                                                    #(s/valid? ::fs/recipe-id %))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace amount-unit))
             [instructions set-instructions!] (uix/use-state (::fs/instructions flavor))
             [mixins set-mixins!] (uix/use-state (::fs/mixins flavor))
             flavor-valid? (and flavor-name-valid?
                                amount-valid?
                                amount-unit-valid?
                                recipe-id-valid?)
             unsaved-changes? (or (and (not= flavor-name (::fs/name flavor))
                                       (not (and (str/blank? flavor-name)
                                                 (str/blank? (::fs/name flavor)))))
                                  (not= (js/parseFloat amount) (::fs/amount flavor))
                                  (not= amount-unit (::fs/amount-unit flavor))
                                  (not= mixins (::fs/mixins flavor))
                                  (not= instructions (::fs/instructions flavor)))]
         (uix/use-effect
           (fn []
             (when (not= amount-unit-type (namespace amount-unit))
               (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                            (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))))))
           [amount-unit amount-unit-type on-amount-unit-change])

         ($ Accordion (if (nil? flavor) {:expanded true} {})
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (if flavor-id flavor-name "New Flavor")))
            ($ AccordionDetails
               ($ Stack {:direction "column"
                         :spacing   1.25}
                  ($ TextField {:label     "Name"
                                :error     (not flavor-name-valid?)
                                :value     flavor-name
                                :on-change on-flavor-name-change})
                  ($ FormControl {:full-width true
                                  :error      (not recipe-id-valid?)}
                     ($ InputLabel "Base Recipe")
                     ($ Select {:value     recipe-id
                                :on-change #(on-recipe-id-change (uuid (.. % -target -value)))}
                        (map #($ MenuItem {:value (str (::rs/id %)) :key (str (::rs/id %))} (::rs/name %)) base-recipes)))
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
                                   (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))))))
                  ($ Typography "Mixins")
                  ($ entity-list {:entity-name     "Mixins"
                                  :entities        mixins
                                  :column-headers  ["Type"
                                                    "Amount"]
                                  :cell-text       (for [mixin mixins]
                                                     [(::rs/name (first (filter #(= (::rs/id %)
                                                                                    (::fs/recipe-id mixin)) recipes)))
                                                      (str (::fs/amount mixin)
                                                           " "
                                                           (name (::fs/amount-unit mixin)))])
                                  :config-metadata {:recipes recipes}
                                  :entity-config   mixin-config
                                  :on-change       set-mixins!})
                  (when edit-instructions-open
                    ($ instructions-dialog {:instructions instructions
                                            :validate-fn  #(s/valid? ::rs/instructions %)
                                            :on-close     #(set-edit-instructions-open! false)
                                            :on-save      #(do (set-instructions! %)
                                                               (set-edit-instructions-open! false))}))
                  ($ Typography
                     "Instructions")
                  ($ Paper
                     ($ List
                        (map-indexed (fn [idx, text]
                                       ($ ListItem {:key text}
                                          ($ ListItemText {:primary (str (inc idx) ") " text)})))
                                     instructions)))
                  ($ Button {:variant  "contained"
                             :on-click #(set-edit-instructions-open! true)}
                     "Edit Instructions")
                  ($ Button {:variant  "contained"
                             :disabled (or (not flavor-valid?)
                                           (not unsaved-changes?))
                             :on-click #(on-save (conj {::fs/name         flavor-name
                                                        ::fs/amount       (js/parseFloat amount)
                                                        ::fs/amount-unit  amount-unit
                                                        ::fs/mixins       (or mixins [])
                                                        ::fs/instructions (or instructions [])
                                                        ::fs/recipe-id    recipe-id}
                                                       (when (some? flavor-id) [::fs/id flavor-id])))}
                     "Save")
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(do (on-flavor-name-change (or (::fs/name flavor) ""))
                                            (on-amount-change (or (::fs/amount flavor) 0))
                                            (if-let [original-amount-unit (::fs/amount-unit flavor)]
                                              (do (set-amount-unit-type! (namespace original-amount-unit))
                                                  (on-amount-unit-change original-amount-unit))
                                              (set-amount-unit-type! (namespace ::volume/c)))
                                            (set-mixins! (::fs/mixins flavor))
                                            (set-instructions! (::fs/instructions flavor)))}
                     "Reset")
                  (when flavor
                    ($ Button {:variant  "contained"
                               :color    "error"
                               :on-click #(on-delete flavor-id)}
                       "Delete")))))))

(defui flavor-list [{:keys [flavors recipes on-change active?]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [new-flavor-key set-new-flavor-key!] (uix/use-state (str (random-uuid)))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column" :display (if active? "block" "none")}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [flavor (sort-by ::fs/name (conj flavors nil))]
              ($ flavor-entry {:flavor    flavor
                               :recipes   recipes
                               :on-save   #((if flavor
                                              ajax/PATCH
                                              ajax/PUT) (str utils/api-url "flavors")
                                            {:params          %
                                             :format          :transit
                                             :response-format :transit
                                             :handler         (fn []
                                                                (set-new-flavor-key! (str (random-uuid)))
                                                                (on-change))
                                             :error-handler   error-handler})
                               :on-delete #(ajax/DELETE (str utils/api-url "flavors")
                                                        {:params          {:id %}
                                                         :format          :transit
                                                         :response-format :transit
                                                         :handler         on-change
                                                         :error-handler   error-handler})
                               :key       (or (::fs/id flavor) new-flavor-key)})))))
