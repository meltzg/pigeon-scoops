(ns pigeon-scoops.apps.recipes
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
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
         ($ Dialog {:open true :on-close on-close :full-screen true}
            ($ DialogTitle "Edit Instructions")
            ($ DialogContent
               ($ TextField {:label      "Instructions"
                             :multiline  true
                             :full-width true
                             :value      new-instructions
                             :on-change  on-new-instructions-change}))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save (convert-instructions new-instructions))
                          :disabled (not new-instructions-valid?)}
                  "Save")))))

(defui ingredient-config [{:keys [entity config-metadata on-save on-close]}]
       (let [{:keys [groceries]} config-metadata
             [ingredient-type ingredient-type-valid? on-ingredient-type-change] (utils/use-validation (or (::rs/ingredient-type entity)
                                                                                                          (::gs/type (first groceries)))
                                                                                                      #(s/valid? ::rs/ingredient-type %))
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::rs/amount entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::rs/amount (js/parseFloat %))))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace (or (::rs/amount-unit entity) ::volume/c)))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::rs/amount-unit entity)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::rs/amount-unit %))]

         (uix/use-effect
           (fn []
             (when (not= amount-unit-type (namespace amount-unit))
               (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                            (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))
                                            (= amount-unit-type (namespace ::ucom/pinch)) (first ucom/other-units)))))
           [amount-unit amount-unit-type on-amount-unit-change])

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Ingredient")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not ingredient-type-valid?)}
                     ($ InputLabel "Type")
                     ($ Select {:value     ingredient-type
                                :on-change #(on-ingredient-type-change (keyword (namespace ::gs/type) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (sort (map ::gs/type groceries)))))
                  ($ TextField {:label     "Amount"
                                :error     (not amount-valid?)
                                :value     amount
                                :on-change on-amount-change})
                  ($ FormControl {:full-width true}
                     ($ InputLabel "Amount type")
                     ($ Select {:value     amount-unit-type
                                :on-change #(set-amount-unit-type! (.. % -target -value))}
                        (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                             (map namespace [::volume/c ::mass/g ::ucom/pinch]))))
                  ($ FormControl {:full-width true
                                  :error      (not amount-unit-valid?)}
                     ($ InputLabel "Amount unit")
                     ($ Select {:value     amount-unit
                                :on-change #(on-amount-unit-change (keyword amount-unit-type (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %))
                             (cond (= amount-unit-type (namespace ::mass/g)) (set (keys mass/conversion-map))
                                   (= amount-unit-type (namespace ::volume/c)) (set (keys volume/conversion-map))
                                   (= amount-unit-type (namespace ::ucom/pinch)) ucom/other-units))))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save {::rs/ingredient-type ingredient-type
                                               ::rs/amount          (js/parseFloat amount)
                                               ::rs/amount-unit     amount-unit})
                          :disabled (not (and ingredient-type-valid?
                                              amount-valid?
                                              amount-unit-valid?))}
                  "Save")))))

(defui recipe-entry [{:keys [recipe groceries on-save on-delete]}]
       (let [recipe-id (::rs/id recipe)
             [edit-instructions-open set-edit-instructions-open!] (uix/use-state false)
             [recipe-name recipe-name-valid? on-recipe-name-change] (utils/use-validation (or (::rs/name recipe) "")
                                                                                          #(s/valid? ::rs/name %))
             [recipe-type recipe-type-valid? on-recipe-type-change] (utils/use-validation (or (::rs/type recipe)
                                                                                              (first rs/recipe-types))
                                                                                          #(s/valid? ::rs/type %))
             [amount amount-valid? on-amount-change] (utils/use-validation (or (::rs/amount recipe) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::rs/amount (js/parseFloat %))))
             [amount-unit amount-unit-valid? on-amount-unit-change] (utils/use-validation (or (::rs/amount-unit recipe)
                                                                                              (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::rs/amount-unit %))
             [amount-unit-type set-amount-unit-type!] (uix/use-state (namespace amount-unit))
             [source source-valid? on-source-change] (utils/use-validation (or (::rs/source recipe) "")
                                                                           #(s/valid? ::rs/source %))
             [ingredients set-ingredients!] (uix/use-state (::rs/ingredients recipe))
             [instructions set-instructions!] (uix/use-state (::rs/instructions recipe))
             recipe-valid? (and recipe-name-valid?
                                recipe-type-valid?
                                amount-valid?
                                amount-unit-valid?
                                source-valid?)
             unsaved-changes? (or (and (not= recipe-name (::rs/name recipe))
                                       (not (and (str/blank? recipe-name)
                                                 (str/blank? (::rs/name recipe)))))
                                  (not= recipe-type (::rs/type recipe))
                                  (not= (js/parseFloat amount) (::rs/amount recipe))
                                  (not= amount-unit (::rs/amount-unit recipe))
                                  (and (not= source (::rs/source recipe))
                                       (not (and (str/blank? source)
                                                 (str/blank? (::rs/source recipe)))))
                                  (not= ingredients (::rs/ingredients recipe))
                                  (not= instructions (::rs/instructions recipe)))]

         (uix/use-effect
           (fn []
             (when (not= amount-unit-type (namespace amount-unit))
               (on-amount-unit-change (cond (= amount-unit-type (namespace ::mass/g)) (first (keys mass/conversion-map))
                                            (= amount-unit-type (namespace ::volume/c)) (first (keys volume/conversion-map))))))
           [amount-unit amount-unit-type on-amount-unit-change])

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
                                :on-change #(on-recipe-type-change (keyword (namespace ::rs/type) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort rs/recipe-types))))
                  ($ TextField {:label     "Amount"
                                :error     (not amount-valid?)
                                :value     amount
                                :on-change on-amount-change})
                  ($ FormControl {:full-width true}
                     ($ InputLabel "Amount type")
                     ($ Select {:value     amount-unit-type
                                :on-change #(set-amount-unit-type! (.. % -target -value))}
                        (map #($ MenuItem {:value % :key %} (last (str/split % #"\.")))
                             (map namespace [::volume/c ::mass/g]))))
                  ($ FormControl {:full-width true
                                  :error      (not amount-unit-valid?)}
                     ($ InputLabel "Amount unit")
                     ($ Select {:value     amount-unit
                                :on-change #(on-amount-unit-change (keyword amount-unit-type (.. % -target -value)))}
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
                                            :on-save      #(do (set-instructions! %)
                                                               (set-edit-instructions-open! false))}))
                  ($ entity-list {:entity-name     "Ingredient"
                                  :entities        ingredients
                                  :column-headers  ["Type"
                                                    "Amount"]
                                  :cell-text       (for [ingredient ingredients]
                                                     [(name (::rs/ingredient-type ingredient))
                                                      (str (::rs/amount ingredient)
                                                           " "
                                                           (name (::rs/amount-unit ingredient)))])
                                  :config-metadata {:groceries groceries}
                                  :entity-config   ingredient-config
                                  :on-change       set-ingredients!})
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
                  ($ Button {:variant  "contained"
                             :disabled (or (not recipe-valid?)
                                           (not unsaved-changes?))
                             :on-click #()}
                     "Save")
                  ($ Button {:variant  "contained"
                             :disabled (not unsaved-changes?)
                             :on-click #(do (on-recipe-name-change (or (::rs/name recipe) ""))
                                            (on-recipe-type-change (or (::rs/type recipe)
                                                                       (first rs/recipe-types)))
                                            (on-amount-change (or (::rs/amount recipe) 0))
                                            (if-let [original-amount-unit (::rs/amount-unit recipe)]
                                              (do (set-amount-unit-type! (namespace original-amount-unit))
                                                  (on-amount-unit-change original-amount-unit))
                                              (do (set-amount-unit-type! (namespace ::volume/c))))
                                            (on-source-change (or (::rs/source recipe) ""))
                                            (set-ingredients! (::rs/ingredients recipe))
                                            (set-instructions! (::rs/instructions recipe)))}
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
                               :groceries groceries
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
