(ns pigeon-scoops.recipes
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/CheckCircle$default" :as CheckCircleIcon]
            ["@mui/icons-material/Cancel$default" :as CancelIcon]
            ["@mui/material" :refer [Button
                                     FormControl
                                     InputLabel
                                     Select
                                     Stack
                                     IconButton
                                     List
                                     ListItemButton
                                     ListItemText
                                     MenuItem
                                     Paper
                                     TableContainer
                                     Table
                                     TableHead
                                     TableBody
                                     TableRow
                                     TableCell
                                     TextField]]))

(def recipe-context (uix/create-context))

(defui with-recipe [{:keys [recipe-id children]}]
       (let [{:keys [token]} (use-token)
             [recipe set-recipe!] (uix/use-state nil)
             [recipe-name set-name!] (uix/use-state (or (:recipe/name recipe) ""))
             [public set-public!] (uix/use-state (:recipe/public recipe))
             [amount set-amount!] (uix/use-state (:recipe/amount recipe))
             [amount-unit set-amount-unit!] (uix/use-state (or (:recipe/amount-unit recipe) ""))
             [source set-source!] (uix/use-state (or (:recipe/source recipe) ""))
             [instructions set-instructions!] (uix/use-state (:recipe/instructions recipe))
             [ingredients set-ingredients!] (uix/use-state (:recipe/ingredients recipe))


             unsaved-changes? (not-every? true? (map #(= ((first %) recipe) (second %)) {:recipe/name         recipe-name
                                                                                         :recipe/public       public
                                                                                         :recipe/amount       amount
                                                                                         :recipe/amount-unit  amount-unit
                                                                                         :recipe/source       source
                                                                                         :recipe/instructions instructions
                                                                                         :recipe/ingredients  ingredients}))
             set-ingredient! #(set-ingredients! (map (fn [i]
                                                       (if (= (:ingredient/id i)
                                                              (:ingredient/id %)) % i))
                                                     ingredients))
             remove-ingredient! (fn [ingredient-id]
                                  (set-ingredients! (remove #(= ingredient-id (:ingredient/id %))
                                                            ingredients)))
             reset! (uix/use-memo #(fn [r]
                                     (set-name! (or (:recipe/name r) ""))
                                     (set-public! (:recipe/public r))
                                     (set-amount! (:recipe/amount r))
                                     (set-amount-unit! (:recipe/amount-unit r))
                                     (set-source! (or (:recipe/source r) ""))
                                     (set-instructions! (:recipe/instructions r))
                                     (set-ingredients! (:recipe/ingredients r)))
                                  [])
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when (and recipe-id token)
               (.then (api/get-recipe token recipe-id {}) (juxt set-recipe! reset!))))
           [reset! refresh? token recipe-id])
         ($ (.-Provider recipe-context) {:value {:recipe             recipe
                                                 :recipe-name        recipe-name
                                                 :set-name!          set-name!
                                                 :public             public
                                                 :set-public!        set-public!
                                                 :amount             amount
                                                 :set-amount!        set-amount!
                                                 :amount-unit        amount-unit
                                                 :set-amount-unit!   set-amount-unit!
                                                 :source             source
                                                 :set-source!        set-source!
                                                 :instructions       instructions
                                                 :set-instructions!  set-instructions!
                                                 :ingredients        ingredients
                                                 :set-ingredient!    set-ingredient!
                                                 :remove-ingredient! remove-ingredient!
                                                 :unsaved-changes?   unsaved-changes?
                                                 :reset!             reset!
                                                 :refresh!           #(set-refresh! (not refresh?))}}
            children)))

(defui recipe-list [{:keys [selected-recipe-id]}]
       (let [{:keys [recipes]} (uix/use-context ctx/recipes-context)
             [filter-text set-filter-text!] (uix/use-state "")
             filtered-recipes (filter #(or (str/blank? filter-text)
                                           (str/includes? (str/lower-case (:recipe/name %))
                                                          (str/lower-case filter-text)))
                                      recipes)]
         ($ Stack {:direction "column"}
            ($ TextField {:label     "Filter"
                          :variant   "outlined"
                          :value     filter-text
                          :on-change #(set-filter-text! (.. % -target -value))})
            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [r (sort-by :recipe/name filtered-recipes)]
                 ($ ListItemButton
                    {:key      (:recipe/id r)
                     :selected (= (:recipe/id r) selected-recipe-id)
                     :on-click #(rfe/push-state :pigeon-scoops.routes/recipe {:recipe-id (:recipe/id r)})}
                    ($ ListItemText {:primary (:recipe/name r)})))))))

(defui recipe-control []
       (let [{:constants/keys [unit-types]} (uix/use-context ctx/constants-context)
             {:keys [recipe
                     recipe-name set-name!
                     public set-public!
                     amount set-amount!
                     amount-unit set-amount-unit!
                     source set-source!
                     instructions set-instructions!
                     ingredients
                     reset!
                     unsaved-changes?]} (uix/use-context recipe-context)
             amount-unit-label-id (str "amount-unit-" (:recipe/id recipe))]

         (uix/use-effect
           (fn []
             (when recipe
               (reset! recipe)))
           [recipe reset!])

         ($ Stack {:direction "column" :spacing 1 :sx (clj->js {:minWidth "50%"})}
            ($ Button {:on-click #(rfe/push-state :pigeon-scoops.routes/recipes)}
               "Back to list")
            ($ TextField {:label     "Name"
                          :value     recipe-name
                          :on-change #(set-name! (.. % -target -value))})
            ($ TextField {:label     "Source"
                          :value     source
                          :on-change #(set-source! (.. % -target -value))})
            ($ Stack {:direction "row" :spacing 1}
               ($ number-field {:value amount :set-value! set-amount! :label "Amount"})
               ($ FormControl
                  ($ InputLabel {:id amount-unit-label-id} "Unit")
                  ($ Select {:label-id  amount-unit-label-id
                             :value     amount-unit
                             :label     "Unit"
                             :on-change #(set-amount-unit! (->> unit-types
                                                                (filter (fn [ut]
                                                                          (= (name ut)
                                                                             (.. % -target -value))))
                                                                (first)))}
                     (for [ut unit-types]
                       ($ MenuItem {:value ut :key ut} (name ut))))))
            ($ TextField {:multiline true
                          :value     (str/join "\n" (map-indexed #(str (inc %1) ") " %2) instructions))
                          :on-change #(set-instructions! (->> (.. % -target -value)
                                                              (str/split-lines)
                                                              (map (fn [inst]
                                                                     (str/replace inst #"^\d+?\)" "")))
                                                              (map str/trim)))})
            ;($ recipe-unit-table {:recipe-id (:recipe/id recipe)
            ;                      :units     units})
            ($ Stack {:direction "row" :spacing 1}
               ($ Button {:variant "contained" :disabled (not unsaved-changes?)} "Save")
               ($ Button {:variant  "contained"
                          :on-click (partial reset! recipe)
                          :disabled (not unsaved-changes?)}
                  "Reset")))))

(defui recipe-view [{:keys [path]}]
       (let [{:keys [recipe-id]} path]
         ($ with-recipe {:recipe-id recipe-id}
            ($ Stack {:direction "row" :spacing 1}
               ($ recipe-list {:selected-recipe-id recipe-id})
               ($ recipe-control)))))

(defui recipe-row [{:keys [recipe]}]
       ($ TableRow
          ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.routes/recipe {:recipe-id (:recipe/id recipe)})}
             (:recipe/name recipe))
          ($ TableCell
             (if (:recipe/public recipe)
               ($ CheckCircleIcon {:color "success"})
               ($ CancelIcon {:color "error"})))
          ($ TableCell
             (str (:recipe/amount recipe) " " (name (:recipe/amount-unit recipe))))
          ($ TableCell
             ($ IconButton {:color    "error"
                            :on-click #(prn "delete" (:recipe/id recipe))}
                ($ DeleteIcon)))))

(defui recipes-table []
       (let [{:keys [recipes]} (uix/use-context ctx/recipes-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Name")
                     ($ TableCell "Public")
                     ($ TableCell "Amount")
                     ($ TableCell "Actions")))
               ($ TableBody
                  (for [r (sort-by :recipe/name recipes)]
                    ($ recipe-row {:key (:recipe/id r) :recipe r})))))))
