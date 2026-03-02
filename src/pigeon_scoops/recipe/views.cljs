(ns pigeon-scoops.recipe.views
  (:require
   ["@mui/icons-material/AddCircle$default" :as AddCircleIcon]
   ["@mui/icons-material/Cancel$default" :as CancelIcon]
   ["@mui/icons-material/CheckCircle$default" :as CheckCircleIcon]
   ["@mui/icons-material/Delete$default" :as DeleteIcon]
   ["@mui/material" :refer [IconButton
                            Table TableBody TableCell TableContainer TableHead
                            TableRow]]
   [pigeon-scoops.recipe.context :as rctx]
   [pigeon-scoops.recipe.forms :refer [recipe-form]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui recipe-view [{:keys [path query]}]
  (let [{:keys [recipe-id]} path
        {:keys [amount amount-unit]} query]
    ($ rctx/with-recipe {:recipe-id recipe-id :scaled-amount amount :scaled-amount-unit amount-unit}
       ($ recipe-form {:recipe-id recipe-id
                       :scaled-amount amount
                       :scaled-amount-unit amount-unit}))))

(defui recipe-row [{:keys [recipe]}]
  (let [{:keys [delete!]} (uix/use-context rctx/recipes-context)]
    ($ TableRow
       ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (:recipe/id recipe)})}
          (or (:recipe/name recipe) "[New Recipe]"))
       ($ TableCell
          (if (:recipe/public recipe)
            ($ CheckCircleIcon {:color "success"})
            ($ CancelIcon {:color "error"})))
       ($ TableCell
          (when (and (:recipe/amount recipe) (:recipe/amount-unit recipe))
            (str (:recipe/amount recipe) " " (name (:recipe/amount-unit recipe)))))
       ($ TableCell
          ($ IconButton {:color    "error"
                         :on-click #(delete! (:recipe/id recipe))}
             ($ DeleteIcon))))))

(defui recipes-table []
  (let [{:keys [recipes new-recipe!]} (uix/use-context rctx/recipes-context)]
    ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                     :overflow  "auto"})}
       ($ Table {:sticky-header true}
          ($ TableHead
             ($ TableRow
                ($ TableCell "Name")
                ($ TableCell "Public")
                ($ TableCell "Amount")
                ($ TableCell
                   "Actions"
                   ($ IconButton {:color    "primary"
                                  :disabled (some keyword? (map :recipe/id recipes))
                                  :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe {:recipe-id (new-recipe!)})}
                      ($ AddCircleIcon)))))
          ($ TableBody
             (for [r (sort-by :recipe/name recipes)]
               ($ recipe-row {:key (:recipe/id r) :recipe r})))))))
