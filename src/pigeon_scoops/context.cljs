(ns pigeon-scoops.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [uix.core :as uix :refer [$ defui]]))

(def constants-context (uix/create-context))
(def groceries-context (uix/create-context))
(def grocery-context (uix/create-context))
(def recipes-context (uix/create-context))

(defui with-constants [{:keys [children]}]
       (let [[constants set-constants!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (.then (api/get-constants) set-constants!))
           [])
         ($ (.-Provider constants-context) {:value constants}
            children)))

(defui with-groceries [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [groceries set-groceries!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (.then (api/get-groceries token) set-groceries!)))
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
             [grocery-name set-name!] (uix/use-state (or (:grocery/name grocery) ""))
             [department set-department!] (uix/use-state (or (:grocery/department grocery) ""))
             [units set-units!] (uix/use-state (:grocery/units grocery))
             unsaved-changes? (not-every? true? (map #(= ((first %) grocery) (second %)) {:grocery/name       grocery-name
                                                                                          :grocery/department department
                                                                                          :grocery/units      units}))
             set-unit! #(set-units! (map (fn [u]
                                           (if (= (:grocery-unit/id u)
                                                  (:grocery-unit/id %)) % u))
                                         units))
             remove-unit! (fn [unit-id]
                            (set-units! (remove #(= unit-id (:grocery-unit/id %))
                                                units)))
             new-unit! #(set-units! (conj units {:grocery-unit/id :new}))
             reset! (uix/use-memo #(fn [g]
                                     (set-name! (or (:grocery/name g) ""))
                                     (set-department! (or (:grocery/department g) ""))
                                     (set-units! (:grocery/units g)))
                                  [])
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (cond
               (keyword? grocery-id)
               ((juxt set-grocery! reset!) {})
               (and grocery-id token)
               (.then (api/get-grocery token grocery-id) (juxt set-grocery! reset!))))
           [reset! refresh? token grocery-id])
         ($ (.-Provider grocery-context) {:value {:grocery          grocery
                                                  :grocery-name     grocery-name
                                                  :set-name!        set-name!
                                                  :department       department
                                                  :set-department!  set-department!
                                                  :units            units
                                                  :set-unit!        set-unit!
                                                  :remove-unit!     remove-unit!
                                                  :new-unit!        new-unit!
                                                  :unsaved-changes? unsaved-changes?
                                                  :reset!           reset!
                                                  :refresh!         #(set-refresh! (not refresh?))}}
            children)))

(defui with-recipes [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [recipes set-recipes!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when token
               (.then (api/get-recipes token) set-recipes!)))
           [token refresh?])
         ($ (.-Provider recipes-context) {:value {:recipes  (apply concat (vals recipes))
                                                  :refresh! #(set-refresh! (not refresh?))}}
            children)))

