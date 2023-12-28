(ns pigeon-scoops.apps.groceries
  (:require [ajax.core :as ajax]
            [pigeon-scoops.utils :refer [api-url drop-nth use-validation]]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.components.grocery-manager :as-alias gm]
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

(defui unit-config [{:keys [initial-unit open? on-save on-close]}]
       (let [[source source-valid? on-source-change] (use-validation (::gm/source initial-unit)
                                                                     #(not (clojure.string/blank? %)))
             [mass mass-valid? on-mass-change] (use-validation (::gm/unit-mass initial-unit)
                                                               #(or (nil? %) (> % 0))
                                                               #(let [v (js/parseFloat %)]
                                                                  (if (not (js/isNaN v))
                                                                    v
                                                                    0)))
             [mass-type mass-type-valid? on-mass-type-change] (use-validation (::gm/unit-mass-type initial-unit)
                                                                              #(some #{%} (keys mass/conversion-map)))]
         ($ Dialog {:open open? :on-close on-close}
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
                        (map #($ MenuItem {:value % :key %} (name %)) (keys mass/conversion-map))))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")))))

(defui grocery-unit-row [{:keys [idx unit on-edit on-delete]}]
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
               ($ unit-config {:initial-unit unit
                               :open?        config-open
                               :on-close     #(set-config-open! false)})
               ($ IconButton {:on-click #(set-config-open! true)}
                  ($ EditIcon))
               ($ IconButton {:color    "error"
                              :on-click on-delete}
                  ($ DeleteIcon))))))

(defui grocery-unit-list [{:keys [initial-units on-change]}]
       (let [[open-unit-dialog set-open-unit-dialog!] (uix/use-state false)]
         ($ :div
            ($ unit-config {:open?    open-unit-dialog
                            :on-close #(set-open-unit-dialog! false)})
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
                                                                   :idx       idx
                                                                   :unit      unit
                                                                   :on-delete #(on-change (drop-nth idx initial-units))}))
                                            initial-units)))))))

(defui grocery-entry [{:keys [item]}]
       (let [[grocery-type set-grocery-type!] (uix/use-state (::gm/type item))
             [description set-description!] (uix/use-state (::gm/description item))
             [units set-units!] (uix/use-state (::gm/units item))]
         ($ Accordion
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (name grocery-type)))
            ($ AccordionDetails
               ($ Stack {:direction "column"
                         :spacing   2}
                  ($ TextField {:label     "Type"
                                :disabled  (some? item)
                                :value     grocery-type
                                :on-change #(set-grocery-type! (.. % -target -value))})
                  ($ TextField {:label     "Description"
                                :multiline true
                                :max-rows  4
                                :value     (or description "")
                                :on-change #(set-description! (.. % -target -value))})
                  ($ grocery-unit-list {:initial-units units :on-change set-units!}))))))

(defui grocery-list [{:keys [groceries on-change]}]
       ($ Stack {:direction "column"}
          (for [item (sort #(compare (::gm/type %1)
                                     (::gm/type %2)) groceries)]
            ($ grocery-entry {:item item :key (::gm/type item)}))))
