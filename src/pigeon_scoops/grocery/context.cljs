(ns pigeon-scoops.grocery.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def groceries-context (uix/create-context))
(def grocery-context (uix/create-context))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-groceries token)
                   (.then set-groceries!))))
           [token refresh?])
         ($ (.-Provider groceries-context) {:value {:groceries    groceries
                                                    :new-grocery! #(do
                                                                     (set-groceries! (conj groceries {:grocery/id :new}))
                                                                     :new)
                                                    :refresh!     #(set-refresh! (not refresh?))}}
            children)))

(defui with-grocery [{:keys [grocery-id children]}]
       (let [{:keys [token]} (use-token)
             [grocery set-grocery!] (uix/use-state nil)
             [editable-grocery set-editable-grocery!] (uix/use-state nil)
             unsaved-changes? (not= grocery editable-grocery)
             set-unit! #(set-editable-grocery! (update editable-grocery
                                                       :grocery/units
                                                       (fn [units]
                                                         (map (fn [u]
                                                                (if (= (:grocery-unit/id u)
                                                                       (:grocery-unit/id %)) % u))
                                                              units))))
             remove-unit! (fn [unit-id]
                            (set-editable-grocery! (update editable-grocery
                                                           :grocery/units
                                                           remove
                                                           #(= unit-id (:grocery-unit/id %)))))
             new-unit! (fn []
                         (set-editable-grocery! (update editable-grocery
                                                        :grocery/units
                                                        #(conj % {:grocery-unit/id :new}))))
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (cond
               (keyword? grocery-id)
               ((juxt set-grocery! set-editable-grocery!) {})
               (and grocery-id token)
               (.then (api/get-grocery token grocery-id) (juxt set-grocery! set-editable-grocery!))))
           [reset! refresh? token grocery-id])
         ($ (.-Provider grocery-context) {:value {:grocery               grocery
                                                  :editable-grocery      editable-grocery
                                                  :set-editable-grocery! set-editable-grocery!
                                                  :set-unit!             set-unit!
                                                  :remove-unit!          remove-unit!
                                                  :new-unit!             new-unit!
                                                  :unsaved-changes?      unsaved-changes?
                                                  :reset!                reset!
                                                  :refresh!              #(set-refresh! (not refresh?))}}
            children)))


