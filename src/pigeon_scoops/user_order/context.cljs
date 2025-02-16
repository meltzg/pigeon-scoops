(ns pigeon-scoops.user-order.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def orders-context (uix/create-context))
(def order-context (uix/create-context))

(defui with-orders [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [orders set-orders!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-orders token)
                   (.then set-orders!))))
           [token refresh?])
         ($ (.-Provider orders-context) {:value {:orders     orders
                                                 :new-order! #(do
                                                                (set-orders! (conj orders {:user-order/id :new}))
                                                                :new)
                                                 :refresh!   #(set-refresh! (not refresh?))}}
            children)))

(defui with-order [{:keys [order-id children]}]
       (let [{:keys [token]} (use-token)
             [order set-order!] (uix/use-state nil)
             [editable-order set-editable-order!] (uix/use-state nil)
             unsaved-changes? (not= order editable-order)
             set-item! #(set-editable-order! (update editable-order
                                                     :user-order/items
                                                     (fn [items] (map (fn [i]
                                                                        (if (= (:order-item/id i)
                                                                               (:order-item/id %)) % i))
                                                                      items))))
             remove-item! (fn [item-id]
                            (set-editable-order! (update editable-order
                                                         :user-order/items
                                                         remove
                                                         #(= item-id (:order-item/id %)))))
             new-item! (fn []
                         (set-editable-order! (update editable-order
                                                      :user-order/items
                                                      #(conj % {:order-item/id :new}))))
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (cond (keyword? order-id)
                   ((juxt set-order! set-editable-order!) {})
                   (and order-id token)
                   (.then (api/get-order token order-id)
                          (juxt set-order! set-editable-order!))))
           [refresh? token order-id])
         ($ (.-Provider order-context) {:value {:order               order
                                                :editable-order      editable-order
                                                :set-editable-order! set-editable-order!
                                                :set-item!           set-item!
                                                :remove-item!        remove-item!
                                                :new-item!           new-item!
                                                :unsaved-changes?    unsaved-changes?
                                                :refresh!            #(set-refresh! (not refresh?))}}
            children)))