(ns pigeon-scoops.user-order.views
  (:require [clojure.string :as str]
            [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.user-order.context :as octx]
            [pigeon-scoops.recipe.context :as rctx]
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
                                     TextField]]))

(defui grocery-list [{:keys [selected-order-id]}]
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
                     :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/order {:order-id (:grocery/id o)})}
                    ($ ListItemText {:primary (or (:user-order/note o) "[New Order]")})))))))


(defui order-item-row [{:keys [order-item]}]
       (let [unit-types (update-keys (->> ctx/constants-context
                                          (uix/use-context)
                                          :constants/unit-types
                                          (group-by namespace))
                                     keyword)
             {:keys [set-unit! remove-item!]} (uix/use-context octx/order-context)]
         ($ TableRow
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-item! (:order-item/id order-item))}
                  ($ DeleteIcon))))))

(defui order-item-table []
       (let [{:keys [units new-unit!]} (uix/use-context gctx/grocery-context)]
         ($ TableContainer {:component Paper}
            ($ Table
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Source")
                     ($ TableCell "Cost")
                     ($ TableCell "Mass")
                     ($ TableCell "Volume")
                     ($ TableCell "Common")
                     ($ TableCell
                        "Actions"
                        ($ IconButton {:color    "primary"
                                       :disabled (some keyword? (map :order-item/id units))
                                       :on-click new-unit!}
                           ($ AddCircleIcon)))))

               ($ TableBody
                  (for [u units]
                    ($ order-item-row {:key (:order-item/id u) :unit u})))))))

(defui order-control []
       (let [{:constants/keys [order-statuses]} (uix/use-context ctx/constants-context)
             {:keys [order note set-note! status set-status! reset! unsaved-changes?]} (uix/use-context octx/orders-context)
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
