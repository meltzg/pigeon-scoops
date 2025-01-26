(ns pigeon-scoops.groceries
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/material" :refer [Stack
                                     List
                                     ListItemButton
                                     ListItemText
                                     TextField]]))

(defui grocery-list [{:keys [selected-grocery-id]}]
       (let [{:keys [groceries]} (uix/use-context ctx/grocery-context)
             [filter-text set-filter-text!] (uix/use-state nil)
             filtered-groceries (filter #(or (str/blank? filter-text)
                                             (str/includes? (str/lower-case (:grocery/name %))
                                                            (str/lower-case filter-text)))
                                        groceries)]
         ($ Stack {:direction "column"}
            ($ TextField {:label     "Filter"
                          :variant   "outlined"
                          :value     filter-text
                          :on-change #(set-filter-text! (.. % -target -value))})
            ($ List
               (for [g (sort-by :grocery/name filtered-groceries)]
                 ($ ListItemButton
                    {:key      (:grocery/id g)
                     :selected (= (:grocery/id g) selected-grocery-id)
                     :on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id g)})}
                    ($ ListItemText {:primary (:grocery/name g)})))))))

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
            ($ grocery-list {:selected-grocery-id grocery-id})
            ($ :div (str grocery)))))
