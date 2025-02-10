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
             [note set-note!] (uix/use-state (or (:user-order/note order) ""))
             [status set-status!] (uix/use-state (or (:user-order/status order) ""))
             [items set-items!] (uix/use-state (:user-order/items order))


             unsaved-changes? (not-every? true? (map #(= ((first %) order) (second %)) {:user-order/note   note
                                                                                        :user-order/status status
                                                                                        :user-order/items  items}))
             set-item! #(set-items! (map (fn [i]
                                           (if (= (:order-item/id i)
                                                  (:order-item/id %)) % i))
                                         items))
             remove-item! (fn [item-id]
                            (set-items! (remove #(= item-id (:order-item/id %))
                                                items)))
             new-item! #(set-items! (conj items {:order-item/id :new}))
             reset! (uix/use-memo #(fn [o]
                                     (set-note! (or (:user-order/note o) ""))
                                     (set-status! (or (:user-order/status o) ""))
                                     (set-items! (:user-order/items o)))
                                  [])
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (cond (keyword? order-id)
                   ((juxt set-order! reset!) {})
                   (and order-id token)
                   (.then (api/get-order token order-id)
                          (juxt set-order! reset!))))
           [reset! refresh? token order-id])
         ($ (.-Provider order-context) {:value {:user-order       order
                                                :note             note
                                                :set-note!        set-note!
                                                :status           status
                                                :set-status!      set-status!
                                                :items            items
                                                :set-item!        set-item!
                                                :remove-item!     remove-item!
                                                :new-item!        new-item!
                                                :unsaved-changes? unsaved-changes?
                                                :reset!           reset!
                                                :refresh!         #(set-refresh! (not refresh?))}}
            children)))