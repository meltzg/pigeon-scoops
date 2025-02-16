(ns pigeon-scoops.user-order.views
  (:require [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.recipe.context :as rctx]
            [pigeon-scoops.user-order.context :as octx]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/icons-material/AddCircle$default" :as AddCircleIcon]
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
                                     TextField
                                     Typography]]))

(defui order-list [{:keys [selected-order-id]}]
       (let [{:keys [orders new-order!]} (uix/use-context octx/orders-context)]
         ($ Stack {:direction "column"}
            ($ IconButton {:color    "primary"
                           :disabled (some keyword? (map :user-order/id orders))
                           :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (new-order!)})}
               ($ AddCircleIcon))

            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [o orders]
                 ($ ListItemButton
                    {:key      (:user-order/id o)
                     :selected (= (:user-order/id o) selected-order-id)
                     :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (:user-order/id o)})}
                    ($ ListItemText {:primary (or (:user-order/note o) "[New Order]")})))))))


(defui order-item-row [{:keys [order-item]}]
       (let [{:constants/keys [unit-types order-statuses]} (uix/use-context ctx/constants-context)
             {:keys [recipes]} (uix/use-context rctx/recipes-context)
             {:keys [set-item! remove-item!]} (uix/use-context octx/order-context)]
         ($ TableRow
            ($ TableCell
               ($ FormControl
                  ($ Select {:value     (str (:order-item/recipe-id order-item))
                             :on-change #(set-item! (assoc order-item :order-item/recipe-id (uuid (.. % -target -value))))}
                     (for [recipe (sort-by :recipe/name recipes)]
                       ($ MenuItem {:value (str (:recipe/id recipe)) :key (:recipe/id recipe)}
                          (:recipe/name recipe))))))
            ($ TableCell
               ($ Stack {:direction "row" :spacing 1}
                  ($ number-field {:value          (:order-item/amount order-item)
                                   :set-value!     #(set-item! (assoc order-item :order-item/amount %))
                                   :hide-controls? true})
                  ($ FormControl
                     ($ Select {:value     (or (:order-item/amount-unit order-item) "")
                                :on-change #(set-item! (assoc order-item :order-item/amount-unit
                                                                         (->> unit-types
                                                                              (filter (fn [ut]
                                                                                        (= (name ut)
                                                                                           (.. % -target -value))))
                                                                              (first))))}
                        (for [ut unit-types]
                          ($ MenuItem {:value ut :key ut} (name ut)))))))
            ($ TableCell
               ($ FormControl
                  ($ Select {:value     (:order-item/status order-item)
                             :on-change #(set-item! (assoc order-item
                                                      :order-item/status
                                                      (keyword "status" (.. % -target -value))))}
                     (for [s order-statuses]
                       ($ MenuItem {:value s :key s} (name s))))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-item! (:order-item/id order-item))}
                  ($ DeleteIcon))))))

(defui order-item-table []
       (let [{:keys [items new-item!]} (uix/use-context octx/order-context)]
         ($ TableContainer {:component Paper}
            ($ Table
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Recipe")
                     ($ TableCell "Amount")
                     ($ TableCell "Status")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :order-item/id items))
                                       :on-click new-item!}
                           ($ AddCircleIcon)))))

               ($ TableBody
                  (for [i items]
                    ($ order-item-row {:key (:order-item/id i) :order-item i})))))))

(defui order-control []
       (let [{:constants/keys [order-statuses]} (uix/use-context ctx/constants-context)
             {:keys [order note set-note! status set-status! reset! unsaved-changes?]} (uix/use-context octx/order-context)
             status-label-id (str "status-" (:user-order/status order))]

         (uix/use-effect
           (fn []
             (when order
               (reset! order)))
           [order reset!])

         ($ Stack {:direction "column" :spacing 1}
            ($ Stack {:direction "row"}
               ($ Button {:on-click #(rfe/push-state :pigeon-scoops.user-order.routes/orders)}
                  "Back to list")
               ($ Button {:disabled (not unsaved-changes?)}
                  "Save")
               ($ Button {:on-click (partial reset! order)
                          :disabled (not unsaved-changes?)}
                  "Reset"))
            ($ TextField {:label     "Note"
                          :value     note
                          :on-change #(set-note! (.. % -target -value))})
            ($ FormControl
               ($ InputLabel {:id status-label-id} "Status")
               ($ Select {:label-id  status-label-id
                          :value     status
                          :label     "Status"
                          :on-change #(set-status! (keyword "status" (.. % -target -value)))}
                  (for [s order-statuses]
                    ($ MenuItem {:value s :key s} (name s)))))
            ($ order-item-table))))

(defui order-view [{:keys [path]}]
       (let [{:keys [order-id]} path]
         ($ octx/with-order {:order-id order-id}
            ($ Stack {:direction "row" :spacing 1}
               ($ order-list {:selected-order-id order-id})
               ($ order-control)))))

(defui order-row [{:keys [order]}]
       ($ TableRow
          ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (:user-order/id order)})}
             (:user-order/note order))
          ($ TableCell
             (name (:user-order/status order)))
          ($ TableCell
             ($ IconButton {:color    "error"
                            :on-click #(prn "delete" (:user-order/id order))}
                ($ DeleteIcon)))))


(defui orders-table []
       (let [{:keys [orders new-order!]} (uix/use-context octx/orders-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Note")
                     ($ TableCell "Status")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :user-order/id orders))
                                       :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (new-order!)})}
                           ($ AddCircleIcon)))))
               ($ TableBody
                  (for [o orders]
                    ($ order-row {:key (:user-order/id o) :order o})))))))
