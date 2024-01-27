(ns pigeon-scoops.components.entry-list
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionDetails
                                     AccordionSummary
                                     Button
                                     Stack
                                     Typography]]))

(defui entry-accordion [{:keys [title entry entry-form id-key name-key config-metadata on-save on-delete]}]
       (let [[changed-entry set-changed-entry!] (uix/use-state entry)
             set-changed-entry-clear-reset! #(set-changed-entry! (dissoc % :reset))
             [valid? set-valid!] (uix/use-state false)]

         ($ Accordion (if (nil? entry) {:expanded true} {})
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (if entry
                               (cond-> (name-key changed-entry)
                                       (keyword? (name-key changed-entry)) name)
                               (str "New " (or title "Entry")))))
            ($ AccordionDetails
               ($ Stack {:direction "column"
                         :spacing   1.25}
                  ($ entry-form {:entry              changed-entry
                                 :config-metadata    config-metadata
                                 :set-valid!         set-valid!
                                 :set-changed-entry! set-changed-entry-clear-reset!
                                 :new?               (nil? entry)})
                  ($ Button {:variant  "contained"
                             :disabled (or (not valid?)
                                           (= entry changed-entry))
                             :on-click #(on-save changed-entry)}
                     "Save")
                  ($ Button {:variant  "contained"
                             :disabled (= entry changed-entry)
                             :on-click (partial set-changed-entry! (assoc entry :reset true))}
                     "Reset")
                  (when entry
                    ($ Button {:variant  "contained"
                               :color    "error"
                               :on-click #(on-delete (id-key entry))}
                       "Delete")))))))

(defui entry-list [{:keys [title entries entry-form id-key name-key sort-key endpoint config-metadata on-change active?]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [new-entry-key set-new-entry-key!] (uix/use-state (str (random-uuid)))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column" :display (if active? "block" "none")}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [entry (sort-by sort-key (conj entries nil))]
              ($ entry-accordion {:title           title
                                  :entry           entry
                                  :entry-form      entry-form
                                  :id-key          id-key
                                  :name-key        name-key
                                  :config-metadata config-metadata
                                  :on-save         #((if entry
                                                       ajax/PATCH
                                                       ajax/PUT) (str utils/api-url endpoint)
                                                     {:params          (dissoc % :reset)
                                                      :format          :transit
                                                      :response-format :transit
                                                      :handler         (fn []
                                                                         (set-new-entry-key! (str (random-uuid)))
                                                                         (on-change))
                                                      :error-handler   error-handler})
                                  :on-delete       #(ajax/DELETE (str utils/api-url endpoint)
                                                                 {:params          {:id %}
                                                                  :format          :transit
                                                                  :response-format :transit
                                                                  :handler         on-change
                                                                  :error-handler   error-handler})
                                  :key             (or (id-key entry) new-entry-key)})))))
