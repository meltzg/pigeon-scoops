(ns pigeon-scoops.groceries
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/material" :refer [FormControl
                                     InputLabel
                                     Select
                                     Stack
                                     List
                                     ListItemButton
                                     ListItemText
                                     MenuItem
                                     TextField]]))

(defui grocery-list [{:keys [selected-grocery-id]}]
       (let [{:keys [groceries]} (uix/use-context ctx/grocery-context)
             [filter-text set-filter-text!] (uix/use-state "")
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

(defui grocery-control [{:keys [grocery]}]
       (let [{:constants/keys [departments]} (uix/use-context ctx/constants-context)
             [grocery-name set-name!] (uix/use-state (or (:grocery/name grocery) ""))
             [department set-department!] (uix/use-state (or (:grocery/department grocery) ""))
             department-label-id (str "department-" (:grocery/id grocery))]

         (uix/use-effect
           (fn []
             (when grocery
               (set-name! (:grocery/name grocery))
               (set-department! (:grocery/department grocery))))
           [grocery])

         ($ Stack {:direction "column"}
            ($ TextField {:label     "Name"
                          :value     grocery-name
                          :on-change #(set-name! (.. % -target -value))})
            ($ FormControl
               ($ InputLabel {:id department-label-id} "Department")
               ($ Select {:label-id  department-label-id
                          :value     department
                          :label     "Department"
                          :on-change #(set-department! (keyword "department" (.. % -target -value)))}
                  (for [d departments]
                    ($ MenuItem {:value d :key d} (name d))))))))

(defui grocery-view [{:keys [path]}]
       (let [{:keys [grocery-id]} path
             [grocery set-grocery!] (uix/use-state nil)
             {:keys [token]} (use-token)]
         (uix/use-effect
           (fn []
             (when grocery-id
               (api/get-grocery token set-grocery! grocery-id)))
           [grocery-id token])
         ($ Stack {:direction "row" :spacing 1}
            ($ grocery-list {:selected-grocery-id grocery-id})
            ($ grocery-control {:grocery grocery}))))
