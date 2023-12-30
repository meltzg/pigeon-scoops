(ns pigeon-scoops.apps.groceries
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.components.grocery-manager :as-alias gm]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.utils :as utils]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/Edit$default" :as EditIcon]
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
                                     IconButton
                                     InputLabel
                                     MenuItem
                                     Paper
                                     Select
                                     Stack
                                     Table
                                     TableBody
                                     TableCell
                                     TableContainer
                                     TableHead
                                     TableRow
                                     TextField
                                     Typography]]))

(defui unit-config [{:keys [initial-unit on-save on-close]}]
       (let [[source source-valid? on-source-change] (utils/use-validation (or (::gm/source initial-unit) "")
                                                                           #(not (str/blank? %)))
             [mass mass-valid? on-mass-change] (utils/use-validation (or (::gm/unit-mass initial-unit) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (> (js/parseFloat %) 0)))
             [mass-type mass-type-valid? on-mass-type-change] (utils/use-validation (or (::gm/unit-mass-type initial-unit) (first (keys mass/conversion-map)))
                                                                                    #(some #{(keyword (namespace ::mass/kg) %)} (keys mass/conversion-map)))
             [volume volume-valid? on-volume-change] (utils/use-validation (or (::gm/unit-volume initial-unit) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (> (js/parseFloat %) 0)))
             [volume-type volume-type-valid? on-volume-type-change] (utils/use-validation (or (::gm/unit-volume-type initial-unit) (first (keys volume/conversion-map)))
                                                                                          #(some #{(keyword (namespace ::volume/c) %)} (keys volume/conversion-map)))
             [common common-valid? on-common-change] (utils/use-validation (or (::gm/unit-common initial-unit) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (> (js/parseFloat %) 0)))
             [common-type common-type-valid? on-common-type-change] (utils/use-validation (or (::gm/unit-common-type initial-unit) (first ucom/other-units))
                                                                                          #(some #{(keyword (namespace ::ucom/pinch) %)} ucom/other-units))
             [cost cost-valid? on-cost-change] (utils/use-validation (or (::gm/unit-cost initial-unit) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (> (js/parseFloat %) 0)))]
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
               ($ Button {:on-click #(on-save (cond-> {::gm/source    source
                                                       ::gm/unit-cost (js/parseFloat cost)}
                                                      mass-valid? (assoc ::gm/unit-mass (js/parseFloat mass)
                                                                         ::gm/unit-mass-type (keyword (namespace ::mass/kg) mass-type))
                                                      volume-valid? (assoc ::gm/unit-volume (js/parseFloat volume)
                                                                           ::gm/unit-volume-type (keyword (namespace ::volume/c) volume-type))
                                                      common-valid? (assoc ::gm/unit-common (js/parseFloat common)
                                                                           ::gm/unit-common-type (keyword (namespace ::ucom/pinch) common-type))))
                          :disabled (not (and source-valid?
                                              cost-valid?
                                              (or (and mass-valid? mass-type-valid?)
                                                  (and volume-valid? volume-type-valid?)
                                                  (and common-valid? common-type-valid?))))}
                  "Save")))))

(defui grocery-unit-row [{:keys [unit on-edit on-delete]}]
       (let [[config-open set-config-open!] (uix/use-state false)]
         ($ TableRow
            ($ TableCell (::gm/source unit))
            ($ TableCell (str (::gm/unit-mass unit) (when (::gm/unit-mass-type unit)
                                                      (name (::gm/unit-mass-type unit)))))
            ($ TableCell (str (::gm/unit-volume unit) (when (::gm/unit-volume-type unit)
                                                        (name (::gm/unit-volume-type unit)))))
            ($ TableCell (str (::gm/unit-common unit) " " (when (::gm/unit-common-type unit)
                                                            (name (::gm/unit-common-type unit)))))
            ($ TableCell (str "$" (::gm/unit-cost unit)))
            ($ TableCell
               (when config-open
                 ($ unit-config {:initial-unit unit
                                 :on-close     #(set-config-open! false)
                                 :on-save      #(do (on-edit %)
                                                    (set-config-open! false))}))
               ($ IconButton {:on-click #(set-config-open! true)}
                  ($ EditIcon))
               ($ IconButton {:color    "error"
                              :on-click on-delete}
                  ($ DeleteIcon))))))

(defui grocery-unit-list [{:keys [initial-units on-change]}]
       (let [[open-unit-dialog set-open-unit-dialog!] (uix/use-state false)]
         ($ Stack {:direction "column" :spacing 1}
            (when open-unit-dialog
              ($ unit-config {:on-close #(set-open-unit-dialog! false)
                              :on-save  #(do (on-change (conj initial-units %))
                                             (set-open-unit-dialog! false))}))
            ($ TableContainer {:component Paper}
               ($ Table
                  ($ TableHead
                     ($ TableRow
                        ($ TableCell "Source")
                        ($ TableCell "Mass")
                        ($ TableCell "Volume")
                        ($ TableCell "Common Unit")
                        ($ TableCell "Cost")
                        ($ TableCell "Actions")))
                  ($ TableBody (map-indexed (fn [idx unit]
                                              ($ grocery-unit-row {:key       idx
                                                                   :unit      unit
                                                                   :on-edit   #(on-change (assoc initial-units idx %))
                                                                   :on-delete #(on-change (vec (utils/drop-nth idx initial-units)))}))
                                            initial-units))))
            ($ Button {:variant  "contained"
                       :on-click #(set-open-unit-dialog! true)}
               ($ AddIcon)
               "Add Unit"))))

(defui grocery-entry [{:keys [item on-save on-delete]}]
       (let [original-type (when item (name (::gm/type item)))
             [grocery-type grocery-type-valid? on-grocery-type-change]
             (utils/use-validation (or original-type "")
                                   #(re-matches #"^[a-zA-Z0-9-]+$" %))
             [description set-description!] (uix/use-state (::gm/description item))
             [units set-units!] (uix/use-state (::gm/units item))
             unsaved-changes? (or (and grocery-type-valid? (not= grocery-type original-type))
                                  (not= description (::gm/description item))
                                  (not= units (::gm/units item)))]
         ($ Accordion
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
                  ($ grocery-unit-list {:initial-units units :on-change set-units!})
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(on-save {::gm/type        (keyword (namespace ::gm/type) grocery-type)
                                                  ::gm/description description
                                                  ::gm/units       units})}
                     "Save")
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(do (set-description! (::gm/description item))
                                            (set-units! (::gm/units item))
                                            (when-not original-type (on-grocery-type-change "")))}
                     "Reset")
                  (when original-type
                    ($ Button {:variant  "contained"
                               :color    "error"
                               :on-click #(on-delete (name grocery-type))}
                       "Delete")))))))

(defui grocery-list [{:keys [groceries on-change]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column"}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [item (sort #(compare (::gm/type %1)
                                       (::gm/type %2)) (conj groceries nil))]
              ($ grocery-entry {:item      item
                                :on-save   #((if item
                                               ajax/PATCH
                                               ajax/PUT) (str utils/api-url "groceries")
                                             {:params          %
                                              :format          :transit
                                              :response-format :transit
                                              :handler         on-change
                                              :error-handler   error-handler})
                                :on-delete #(ajax/DELETE (str utils/api-url "groceries")
                                                         {:params          {:type %}
                                                          :format          :transit
                                                          :response-format :transit
                                                          :handler         on-change
                                                          :error-handler   error-handler})
                                :key       (or (::gm/type item) -1)})))))
