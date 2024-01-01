(ns pigeon-scoops.components.entity-list
  (:require [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/material" :refer [Button
                                     Paper
                                     Stack
                                     Table
                                     TableBody
                                     TableCell
                                     TableContainer
                                     TableHead
                                     TableRow]]))

(defui entity-list [{:keys [entities column-headers entity-config entity-row on-change]}]
       (let [[open-entity-dialog? set-open-entity-dialog!] (uix/use-state false)]
         ($ Stack {:direction "column" :spacing 1}
            (when open-entity-dialog?
              ($ entity-config {:on-close #(set-open-entity-dialog! false)
                                :on-save  #(do (on-change (conj entities %))
                                               (set-open-entity-dialog! false))}))
            ($ TableContainer {:component Paper}
               ($ Table
                  ($ TableHead
                     ($ TableRow
                        (for [header column-headers]
                          ($ TableCell {:key header} header))))
                  ($ TableBody
                     (map-indexed (fn [idx, entity]
                                    ($ entity-row {:key       idx
                                                   :entity    entity
                                                   :on-edit   #(on-change (assoc entities idx %))
                                                   :on-delete #(on-change (vec (utils/drop-nth idx entities)))}))
                                  entities))))
            ($ Button {:variant  "contained"
                       :on-click #(set-open-entity-dialog! true)}
               ($ AddIcon)
               "Add Unit"))))