(ns pigeon-scoops.apps.recipes
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.components.grocery-manager :as-alias gm]
            [pigeon-scoops.components.recipe-manager :as-alias rm]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/Edit$default" :as EditIcon]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionActions
                                     AccordionDetails
                                     AccordionSummary
                                     Stack
                                     Typography]]))

(defui recipe-entry [{:keys [recipe on-save on-delete]}]
       (let [[recipe-name recipe-name-valid? on-recipe-name-change] (utils/use-validation (::rm/name recipe)
                                                                                          #(not (str/blank? %)))]
         ($ Accordion (if (nil? recipe) {:expanded true} {})
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (or recipe-name "New Recipe"))))))

(defui recipe-list [{:keys [recipes groceries on-change]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [new-recipe-key set-new-recipe-key!] (uix/use-state (str (random-uuid)))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column"}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [recipe (sort #(compare (::rm/name %1)
                                         (::rm/name %2)) (conj recipes nil))]
              ($ recipe-entry {:recipe    recipe
                               :on-save   #((if recipe
                                              ajax/PATCH
                                              ajax/PUT) (str utils/api-url "recipes")
                                            {:params          %
                                             :format          :transit
                                             :response-format :transit
                                             :handler         (fn []
                                                                (set-new-recipe-key! (str (random-uuid)))
                                                                (on-change))
                                             :error-handler   error-handler})
                               :on-delete #(ajax/DELETE (str utils/api-url "recipes")
                                                        {:params          {:id %}
                                                         :format          :transit
                                                         :response-format :transit
                                                         :handler         on-change
                                                         :error-handler   error-handler})
                               :key       (or (::rm/id recipe) new-recipe-key)})))))
