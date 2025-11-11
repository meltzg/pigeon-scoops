(ns pigeon-scoops.menu.context
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.hooks :refer [use-token]]
            [pigeon-scoops.utils :refer [determine-ops]]
            [uix.core :as uix :refer [$ defui]]))

(def menus-context (uix/create-context))
(def menu-context (uix/create-context))

(defn save-menu-items! [token menu-id current-items editable-items]
  (let [item-ops (determine-ops :menu-item/id
                                current-items
                                editable-items)]
    (js/Primise.all
      (clj->js
        (concat
          (map (fn [item-to-delete]
                 (-> (js/Primise.all
                       (clj->js (map (comp (partial api/delete-menu-item-size token menu-id)
                                           :menu-item-size/id)
                                     (:menu-item/sizes item-to-delete))))
                     (.then (fn [] (api/delete-menu-item token menu-id (:menu-item/id item-to-delete))))))
               (:delete item-ops))
          (map (fn [new-item]
                 (-> (api/create-menu-item token menu-id new-item)
                     (.then :id)
                     (.then (fn [item-id]
                              (js/Promise.all
                                (clj->js (map (comp (partial api/create-menu-item-size token menu-id)
                                                    #(update % :menu-item-size/menu-item-id item-id))
                                              (:menu-item/sizes new-item))))))))
               (:new item-ops))
          (map (fn [item-to-update])
               (:update item-ops)))))))



(defui with-menus [{:keys [children]}]
       (let [{:keys [token]} (use-token)
             [menus set-menus!] (uix/use-state nil)
             [refresh? set-refresh!] (uix/use-state nil)
             new-menu! #(do
                          (set-menus! (conj menus {:menu/id :new}))
                          :new)
             refresh! #(set-refresh! (not refresh?))
             delete! (fn [menu-id]
                       (-> (api/delete-menu token menu-id)
                           (.then refresh!)))]
         (uix/use-effect
           (fn []
             (when token
               (-> (api/get-menus token)
                   (.then set-menus!))))
           [token refresh?])
         ($ (.-Provider menus-context) {:value {:menus menus
                                                :new-menu! new-menu!
                                                :refresh! refresh!
                                                :delete! delete!}}
            children)))

(defui with-menu [{:keys [menu children]}]
       (let [{:keys [token]} (use-token)
             refresh-menus! (:refresh! (uix/use-context menus-context))
             [menu set-menu!] (uix/use-state menu)
             [editable-menu set-editable-menu!] (uix/use-state menu)
             [refresh? set-refresh!] (uix/use-state nil)
             unsaved-changes? (not= menu editable-menu)
             set-item! #(set-editable-menu!
                          (update editable-menu
                                  :menu/items
                                  (fn [items]
                                    (map (fn [i]
                                           (if (= (:menu-item/id i)
                                                  (:menu-item/id %))
                                             %
                                             i))
                                         items))))
             remove-item! (fn [item-id]
                            (set-editable-menu! (update editable-menu
                                                        :menu/items
                                                        (partial
                                                          remove
                                                          #(= item-id (:menu/id %))))))
             new-item! (fn []
                         (set-editable-menu! (update editable-menu
                                                     :menu/items
                                                     #(conj % {:menu/id (random-uuid)}))))
             set-item-size! (fn [item-size]
                              (set-editable-menu!
                                (update editable-menu
                                        :menu/items
                                        (fn [items]
                                          (map (fn [item]
                                                 (if (= (:menu-item/id item)
                                                        (:menu-item-size/menu-item-id item-size))
                                                   (update item :menu-item/sizes
                                                           (fn [item-sizes]
                                                             (map #(if (= (:menu-item-size/id %)
                                                                          (:menu-item-size/id item-size))
                                                                     item-size
                                                                     %)
                                                                  item-sizes)))
                                                   item))
                                               items)))))
             remove-item-size! (fn [item-id item-size-id]
                                 (set-editable-menu!
                                   (update editable-menu
                                           :menu/items
                                           (fn [items]
                                             (map (fn [item]
                                                    (if (= (:menu-item/id item)
                                                           item-id)
                                                      (update item :menu-item/sizes
                                                              (partial remove
                                                                       #(= item-size-id
                                                                           (:menu-item-size/id %))))
                                                      item))
                                                  items)))))
             new-item-size! (fn [item-id]
                              (set-editable-menu!
                                (update editable-menu
                                        :menu/items
                                        (fn [items]
                                          (map (fn [item]
                                                 (if (= (:menu-item/id item)
                                                        item-id)
                                                   (update item :menu-item/sizes
                                                           #(conj % {:menu-item-size/menu-item-id (:menu-item/id %)
                                                                     :menu-item-size/id (random-uuid)}))
                                                   item))
                                               items)))))
             save! (fn []
                     (let [item-ops (determine-ops :menu-item/id
                                                   (:menu/items menu)
                                                   (:menu/items editable-menu))
                           menu-id (atom (:menu/id editable-menu))]
                       (-> (if (uuid? (:menu/id editable-menu))
                             (api/update-menu token editable-menu)
                             (-> (api/create-menu token editable-menu)
                                 (.then #(do (refresh-menus!)
                                             (swap! menu-id (:id %))))))
                           (.then (fn [_]
                                    (js/Promise.all (clj->js (concat
                                                               (map (partial api/create-menu-item token @menu-id) (:new item-ops))
                                                               (map (partial api/update-menu-item token @menu-id) (:update item-ops))
                                                               (map (partial api/delete-menu-item token @menu-id) (:delete item-ops)))))))
                           (.then #(set-refresh! (not refresh?))))))]
         (uix/use-effect
           (fn []))))




