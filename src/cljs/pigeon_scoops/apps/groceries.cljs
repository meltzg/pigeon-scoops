(ns pigeon-scoops.apps.groceries
  (:require [ajax.core :as ajax]
            [pigeon-scoops.utils :refer [api-url drop-nth]]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
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
                                     IconButton
                                     Paper
                                     Stack
                                     Table
                                     TableBody
                                     TableCell
                                     TableContainer
                                     TableHead
                                     TableRow
                                     TextField
                                     Typography]]))

(defui grocery-unit-list [{:keys [initial-units on-change]}]
       (let [[units set-units!] (uix/use-state initial-units)]
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
                                           ($ TableRow {:key idx}
                                              ($ TableCell (::gm/source unit))
                                              ($ TableCell (str (::gm/unit-mass unit) (when (::gm/unit-mass-type unit)
                                                                                        (name (::gm/unit-mass-type unit)))))
                                              ($ TableCell (str (::gm/unit-volume unit) (when (::gm/unit-volume-type unit)
                                                                                          (name (::gm/unit-volume-type unit)))))
                                              ($ TableCell (str (::gm/unit-common unit) " " (when (::gm/unit-common-type unit)
                                                                                              (name (::gm/unit-common-type unit)))))
                                              ($ TableCell (str "$" (::gm/unit-cost unit)))
                                              ($ TableCell
                                                 ($ IconButton
                                                    ($ EditIcon))
                                                 ($ IconButton {:color    "error"
                                                                :on-click #(set-units! (drop-nth idx units
                                                                                                 ))}
                                                    ($ DeleteIcon)))))
                                         units))))))

(defui grocery-entry [{:keys [item]}]
       (let [[grocery-type set-grocery-type!] (uix/use-state (::gm/type item))
             [description set-description!] (uix/use-state (::gm/description item))]
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
                  ($ grocery-unit-list {:initial-units (::gm/units item)}))))))

(defui grocery-list [{:keys [groceries on-change]}]
       ($ Stack {:direction "column"}
          (for [item (sort #(compare (::gm/type %1)
                                     (::gm/type %2)) groceries)]
            ($ grocery-entry {:item item :key (::gm/type item)}))))
