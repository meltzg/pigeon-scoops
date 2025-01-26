(ns pigeon-scoops.groceries
  (:require [pigeon-scoops.api :as api]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/material" :refer [Stack
                                     List
                                     ListItemButton
                                     ListItemText]]))

(defui grocery-list []
       (let [{:keys [groceries]} (uix/use-context ctx/grocery-context)]
         ($ List
            (for [g (sort-by :grocery/name groceries)]
              ($ ListItemButton
                 {:key (:grocery/id g)
                  :on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id g)})}
                 ($ ListItemText {:primary (:grocery/name g)}))))))

(defui grocery-view [{:keys [path]}]
       (let [{:keys [grocery-id]} path
             [grocery set-grocery!] (uix/use-state nil)
             {:keys [token]} (use-token)]
         (uix/use-effect
           (fn []
             (when grocery-id
               (api/get-grocery token set-grocery! grocery-id)))
           [grocery-id token])
         ($ Stack {:direction "row"}
            ($ grocery-list)
            ($ :div (str grocery)))))
