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
                                     TableRow
                                     Tooltip]]))

(defui entity-row [{:keys [entity cell-text cell-actions config-metadata entity-config frozen? on-edit on-delete]}]
       (let [[open-entity-dialog? set-open-entity-dialog!] (uix/use-state false)]
         ($ TableRow
            (for [text cell-text]
              ($ TableCell {:key (random-uuid) :style {:white-space "pre-wrap"}} text))
            ($ TableCell
               (when open-entity-dialog?
                 ($ entity-config {:entity          entity
                                   :config-metadata config-metadata
                                   :on-close        #(set-open-entity-dialog! false)
                                   :on-save         #(do (on-edit %)
                                                         (set-open-entity-dialog! false))}))
               cell-actions
               (when-not frozen?
                 [($ Tooltip {:title "Edit"}
                     ($ IconButton {:on-click #(set-open-entity-dialog! true)}
                        ($ EditIcon)))
                  ($ Tooltip {:title "Delete"}
                     ($ IconButton {:color    "error"
                                    :on-click on-delete}
                        ($ DeleteIcon)))])))))

(defui entity-list [{:keys [entity-name entities column-headers cell-text cell-action config-metadata entity-config frozen? on-change]}]
       (let [[open-entity-dialog? set-open-entity-dialog!] (uix/use-state false)]
         ($ Stack {:direction "column" :spacing 1}
            (when open-entity-dialog?
              ($ entity-config {:config-metadata config-metadata
                                :on-close        #(set-open-entity-dialog! false)
                                :on-save         #(do (on-change (conj entities %))
                                                      (set-open-entity-dialog! false))}))
            ($ TableContainer {:component Paper}
               ($ Table
                  ($ TableHead
                     ($ TableRow
                        (for [header (conj (vec column-headers) "Actions")]
                          ($ TableCell {:key header} header))))
                  ($ TableBody
                     (map-indexed (fn [idx, [entity text action]]
                                    ($ entity-row {:key             (random-uuid)
                                                   :entity          entity
                                                   :cell-text       text
                                                   :cell-actions    action
                                                   :entity-config   entity-config
                                                   :config-metadata config-metadata
                                                   :frozen?         frozen?
                                                   :on-edit         #(on-change (assoc (vec entities) idx %))
                                                   :on-delete       #(on-change (vec (utils/drop-nth idx entities)))}))
                                  (map vector entities cell-text (or cell-action (repeat nil)))))))
            (when-not frozen?
              ($ Button {:variant  "contained"
                         :on-click #(set-open-entity-dialog! true)}
                 ($ AddIcon)
                 (str "Add " entity-name))))))