(ns pigeon-scoops.apps.recipes
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
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

(defui instructions-dialog [{:keys [instructions on-save on-close]}]
       (let [convert-instructions #(remove str/blank? (str/split-lines %))
             [new-instructions new-instructions-valid? on-new-instructions-change] (utils/use-validation (str/join "\n" instructions)
                                                                                                         #(s/valid? ::rs/instructions (convert-instructions %)))]
         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Instructions")
            ($ DialogContent
               ($ TextField {:label     "Instructions"
                             :multiline true
                             :max-rows  15
                             :value     new-instructions
                             :on-change on-new-instructions-change}))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save (convert-instructions new-instructions))
                          :disabled (not new-instructions-valid?)}
                  "Save")))))

(defui recipe-entry [{:keys [recipe on-save on-delete]}]
       (let [recipe-id (::rs/id recipe)
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)
             [recipe-name recipe-name-valid? on-recipe-name-change] (utils/use-validation (or (::rs/name recipe) "")
                                                                                          #(s/valid? ::rs/name %))
             [recipe-type recipe-type-valid? on-recipe-type-change] (utils/use-validation (or (::rs/type recipe)
                                                                                              (first rs/recipe-types))
                                                                                          #(s/valid? ::rs/type (keyword (namespace ::rs/type) %)))
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::rs/amount recipe) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::rs/amount (js/parseFloat %))))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace ::volume/c))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::rs/amount-unit recipe)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::rs/amount-unit (keyword amount-unit-type %)))
             [source source-valid? on-source-change] (utils/use-validation (or (::rs/source recipe) "")
                                                                           #(s/valid? ::rs/source %))
             [instructions instructions-valid? on-instructions-change] (utils/use-validation (::rs/instructions recipe)
                                                                                             #(s/valid? ::rs/instructions %))]

         (uix/use-effect
           (fn []
             (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                          (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map)))))
           [amount-unit-type on-amount-unit-change])

         ($ Accordion (if (nil? recipe) {:expanded true} {})
            ($ AccordionSummary {:expandIcon ($ ExpandMoreIcon)}
               ($ Typography (if recipe-id recipe-name "New Recipe")))
            ($ AccordionDetails
               ($ Stack {:direction "column"
                         :spacing   1.25}
                  ($ TextField {:label     "Name"
                                :error     (not recipe-name-valid?)
                                :value     recipe-name
                                :on-change on-recipe-name-change})
                  ($ FormControl {:full-width true
                                  :error      (not recipe-type-valid?)}
                     ($ InputLabel "Recipe type")
                     ($ Select {:value     recipe-type
                                :on-change on-recipe-type-change}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort rs/recipe-types))))
                  ($ TextField {:label     "Amount"
                                :error     (not amount-valid?)
                                :value     amount
                                :on-change on-amount-change})
                  ($ FormControl {:full-width true}
                     ($ InputLabel "Amount type")
                     ($ Select {:value     amount-unit-type
                                :on-change #(let [new-type (.. % -target -value)]
                                              (on-amount-unit-change (cond (= new-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                                                           (= new-type (namespace ::volume/c)) (first (keys volume/conversion-map))))
                                              (set-amount-unit-type! new-type))}
                        (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                             (map namespace [::volume/c ::mass/g]))))
                  ($ FormControl {:full-width true
                                  :error      (not amount-unit-valid?)}
                     ($ InputLabel "Amount unit")
                     ($ Select {:value     amount-unit
                                :on-change on-amount-unit-change}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                                   (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))))))
                  ($ TextField {:label     "Source"
                                :error     (not source-valid?)
                                :value     source
                                :on-change on-source-change})
                  (when edit-instructions-open
                    ($ instructions-dialog {:instructions instructions
                                            :on-close     #(set-edit-instructions-open! false)
                                            :on-save      #(do (on-instructions-change %)
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
                  ($ Button {:variant "contained"}
                     "Save")
                  ($ Button {:variant "contained"}
                     "Reset")
                  (when recipe
                    ($ Button {:variant  "contained"
                               :color    "error"
                               :on-click #(on-delete recipe-id)}
                       "Delete")))))))

(defui recipe-list [{:keys [recipes groceries on-change active?]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [new-recipe-key set-new-recipe-key!] (uix/use-state (str (random-uuid)))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column" :display (if active? "block" "none")}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            (for [recipe (sort #(compare (::rs/name %1)
                                         (::rs/name %2)) (conj recipes nil))]
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
                               :key       (or (::rs/id recipe) new-recipe-key)})))))
