(ns pigeon-scoops.apps.groceries
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.utils :as utils]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionActions
                                     AccordionDetails
                                     AccordionSummary
                                     Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     FormControl
                                     InputLabel
                                     MenuItem
                                     Select
                                     Stack
                                     TextField
                                     Typography]]))

(defui unit-config [{:keys [entity on-save on-close]}]
       (let [[source source-valid? on-source-change] (utils/use-validation (or (::gs/source entity) "")
                                                                           #(s/valid? ::gs/source %))
             [mass mass-valid? on-mass-change] (utils/use-validation (or (::gs/unit-mass entity) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (s/valid? ::gs/unit-mass (js/parseFloat %))))
             [mass-type mass-type-valid? on-mass-type-change] (utils/use-validation (or (::gs/unit-mass-type entity) (first (keys mass/conversion-map)))
                                                                                    #(s/valid? ::gs/unit-mass-type (keyword (namespace ::mass/kg) %)))
             [volume volume-valid? on-volume-change] (utils/use-validation (or (::gs/unit-volume entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::gs/unit-volume (js/parseFloat %))))
             [volume-type volume-type-valid? on-volume-type-change] (utils/use-validation (or (::gs/unit-volume-type entity) (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::gs/unit-volume-type (keyword (namespace ::volume/c) %)))
             [common common-valid? on-common-change] (utils/use-validation (or (::gs/unit-common entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::gs/unit-common (js/parseFloat %))))
             [common-type common-type-valid? on-common-type-change] (utils/use-validation (or (::gs/unit-common-type entity) (first ucom/other-units))
                                                                                          #(s/valid? ::gs/unit-common-type (keyword (namespace ::ucom/pinch) %)))
             [cost cost-valid? on-cost-change] (utils/use-validation (or (::gs/unit-cost entity) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (s/valid? ::gs/unit-cost (js/parseFloat %))))]
         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Configure Unit")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ TextField {:label     "Source"
                                :value     source
                                :error     (not source-valid?)
                                :on-change on-source-change})
                  ($ TextField {:label     "Mass/Weight"
                                :value     mass
                                :error     (not mass-valid?)
                                :on-change on-mass-change})
                  ($ FormControl {:full-width true
                                  :error      (not mass-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     mass-type
                                :on-change on-mass-type-change}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort (keys mass/conversion-map)))))
                  ($ TextField {:label     "Volume"
                                :value     volume
                                :error     (not volume-valid?)
                                :on-change on-volume-change})
                  ($ FormControl {:full-width true
                                  :error      (not volume-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     volume-type
                                :on-change on-volume-type-change}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort (keys volume/conversion-map)))))
                  ($ TextField {:label     "Common units"
                                :value     common
                                :error     (not common-valid?)
                                :on-change on-common-change})
                  ($ FormControl {:full-width true
                                  :error      (not common-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     common-type
                                :on-change on-common-type-change}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort ucom/other-units))))
                  ($ TextField {:label     "Cost ($)"
                                :value     cost
                                :error     (not cost-valid?)
                                :on-change on-cost-change})))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save (cond-> {::gs/source    source
                                                       ::gs/unit-cost (js/parseFloat cost)}
                                                      mass-valid? (assoc ::gs/unit-mass (js/parseFloat mass)
                                                                         ::gs/unit-mass-type (keyword (namespace ::mass/kg) mass-type))
                                                      volume-valid? (assoc ::gs/unit-volume (js/parseFloat volume)
                                                                           ::gs/unit-volume-type (keyword (namespace ::volume/c) volume-type))
                                                      common-valid? (assoc ::gs/unit-common (js/parseFloat common)
                                                                           ::gs/unit-common-type (keyword (namespace ::ucom/pinch) common-type))))
                          :disabled (not (and source-valid?
                                              cost-valid?
                                              (or (and mass-valid? mass-type-valid?)
                                                  (and volume-valid? volume-type-valid?)
                                                  (and common-valid? common-type-valid?))))}
                  "Save")))))

(defui grocery-entry [{:keys [item on-save on-delete]}]
       (let [original-type (when item (name (::gs/type item)))
             [grocery-type grocery-type-valid? on-grocery-type-change]
             (utils/use-validation (or original-type "")
                                   #(re-matches #"^[a-zA-Z0-9-]+$" %))
             [description set-description!] (uix/use-state (::gs/description item))
             [units set-units!] (uix/use-state (::gs/units item))
             unsaved-changes? (or (and grocery-type-valid? (not= grocery-type original-type))
                                  (and (not= description (::gs/description item))
                                       (not (and (str/blank? description)
                                                 (str/blank? (::gs/description item)))))
                                  (not= units (::gs/units item)))]
         ($ Accordion (if (nil? item) {:expanded true} {})
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (or original-type "New Grocery Item")))
            ($ AccordionDetails
               ($ Stack {:direction "column"
                         :spacing   1}
                  ($ TextField {:label     "Type"
                                :error     (not grocery-type-valid?)
                                :disabled  (some? item)
                                :value     grocery-type
                                :on-change on-grocery-type-change})
                  ($ TextField {:label     "Description"
                                :multiline true
                                :max-rows  4
                                :value     (or description "")
                                :on-change #(set-description! (.. % -target -value))})
                  ($ entity-list {:entity-name    "Unit"
                                  :entities       units
                                  :column-headers ["Source"
                                                   "Mass"
                                                   "Volume"
                                                   "Common Unit"
                                                   "Cost"]
                                  :cell-text      (for [unit units]
                                                    [(::gs/source unit)
                                                     (str (::gs/unit-mass unit) (when (::gs/unit-mass-type unit)
                                                                                  (name (::gs/unit-mass-type unit))))
                                                     (str (::gs/unit-volume unit) (when (::gs/unit-volume-type unit)
                                                                                    (name (::gs/unit-volume-type unit))))
                                                     (str (::gs/unit-common unit) " " (when (::gs/unit-common-type unit)
                                                                                        (name (::gs/unit-common-type unit))))
                                                     (str "$" (::gs/unit-cost unit))])
                                  :entity-config  unit-config
                                  :on-change      set-units!})
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(on-save (conj {::gs/type  (keyword (namespace ::gs/type) grocery-type)
                                                        ::gs/units (or units [])}
                                                       (when-not (str/blank? description) [::gs/description description])))}
                     "Save")
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(do (set-description! (::gs/description item))
                                            (set-units! (::gs/units item))
                                            (when-not original-type (on-grocery-type-change "")))}
                     "Reset")
                  (when original-type
                    ($ Button {:variant  "contained"
                               :color    "error"
                               :on-click #(on-delete (name grocery-type))}
                       "Delete")))))))

(defui grocery-list [{:keys [groceries on-change active?]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [new-item-key set-new-item-key!] (uix/use-state (str (random-uuid)))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column" :display (if active? "block" "none")}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [item (sort #(compare (::gs/type %1)
                                       (::gs/type %2)) (conj groceries nil))]
              ($ grocery-entry {:item      item
                                :on-save   #((if item
                                               ajax/PATCH
                                               ajax/PUT) (str utils/api-url "groceries")
                                             {:params          %
                                              :format          :transit
                                              :response-format :transit
                                              :handler         (fn []
                                                                 (set-new-item-key! (str (random-uuid)))
                                                                 (on-change))
                                              :error-handler   error-handler})
                                :on-delete #(ajax/DELETE (str utils/api-url "groceries")
                                                         {:params          {:type %}
                                                          :format          :transit
                                                          :response-format :transit
                                                          :handler         on-change
                                                          :error-handler   error-handler})
                                :key       (or (::gs/type item) new-item-key)})))))
