(ns pigeon-scoops.components.entity-list
  (:require [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/Edit$default" :as EditIcon]
            ["@mui/material" :refer [Button
                                     IconButton
                                     Paper
                                     Stack
                                     Table
                                     TableBody
                                     TableCell
                                     TableContainer
                                     TableHead
                                     TableRow]]))

(defui entity-row [{:keys [entity cell-text entity-config on-edit on-delete]}]
       (let [[open-entity-dialog? set-open-entity-dialog!] (uix/use-state false)]
         ($ TableRow
            (for [text cell-text]
              ($ TableCell {:key (random-uuid)} text))
            ($ TableCell
               (when open-entity-dialog?
                 ($ entity-config {:entity   entity
                                   :on-close #(set-open-entity-dialog! false)
                                   :on-save  #(do (on-edit %)
                                                  (set-open-entity-dialog! false))}))
               ($ IconButton {:on-click #(set-open-entity-dialog! true)}
                  ($ EditIcon))
               ($ IconButton {:color    "error"
                              :on-click on-delete}
                  ($ DeleteIcon))))))

(defui entity-list [{:keys [entities column-headers cell-text entity-config on-change]}]
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
                     (map-indexed (fn [idx, [entity text]]
                                    ($ entity-row {:key           (random-uuid)
                                                   :entity        entity
                                                   :cell-text     text
                                                   :entity-config entity-config
                                                   :on-edit       #(on-change (assoc entities idx %))
                                                   :on-delete     #(on-change (vec (utils/drop-nth idx entities)))}))
                                  (map vector entities cell-text)))))
            ($ Button {:variant  "contained"
                       :on-click #(set-open-entity-dialog! true)}
               ($ AddIcon)
               "Add Unit"))))