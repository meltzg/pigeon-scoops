(ns pigeon-scoops.groceries
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.components.number-field :refer [number-field]]
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
                                     Paper
                                     TableContainer
                                     Table
                                     TableHead
                                     TableBody
                                     TableRow
                                     TableCell
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
            ($ List {:sx (clj->js {:maxHeight "100vh"
                                   :overflow  "auto"})}
               (for [g (sort-by :grocery/name filtered-groceries)]
                 ($ ListItemButton
                    {:key      (:grocery/id g)
                     :selected (= (:grocery/id g) selected-grocery-id)
                     :on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id g)})}
                    ($ ListItemText {:primary (:grocery/name g)})))))))


(defui grocery-unit-row [{:keys [grocery-id unit]}]
       (let [unit-types (update-keys (->> ctx/constants-context
                                          (uix/use-context)
                                          :constants/unit-types
                                          (group-by namespace))
                                     keyword)

             [source set-source!] (uix/use-state (:grocery-unit/source unit))
             [cost set-cost!] (uix/use-state (:grocery-unit/unit-cost unit))
             [unit-mass set-unit-mass!] (uix/use-state (or (:grocery-unit/unit-mass unit) 0))
             [unit-mass-type set-unit-mass-type!] (uix/use-state (or (:grocery-unit/unit-mass-type unit) ""))
             [unit-volume set-unit-volume!] (uix/use-state (or (:grocery-unit/unit-volume unit) 0))
             [unit-volume-type set-unit-volume-type!] (uix/use-state (or (:grocery-unit/unit-volume-type unit) ""))
             [unit-common set-unit-common!] (uix/use-state (or (:grocery-unit/unit-common unit) 0))
             [unit-common-type set-unit-common-type!] (uix/use-state (or (:grocery-unit/unit-common-type unit) ""))]
         ($ TableRow
            ($ TableCell
               ($ TextField {:value     source
                             :on-change #(set-source! (.. % -target -value))}))
            ($ TableCell
               ($ number-field {:value cost :set-value! set-cost!}))
            (for [[idx [val set-val! val-unit set-val-unit! option-key]]
                  (map-indexed vector [[unit-mass set-unit-mass! unit-mass-type set-unit-mass-type! :mass]
                                       [unit-volume set-unit-volume! unit-volume-type set-unit-volume-type! :volume]
                                       [unit-common set-unit-common! unit-common-type set-unit-common-type! :common]])]

              ($ TableCell {:key idx}
                 ($ Stack {:direction "row" :spacing 1}
                    ($ number-field {:value val :set-value! set-val!})
                    ($ FormControl
                       ($ Select {:value     val-unit
                                  :on-change #(set-val-unit! (keyword (name option-key) (.. % -target -value)))}
                          (for [o (option-key unit-types)]
                            ($ MenuItem {:value o :key o} (name o)))))))))))



(defui grocery-unit-table [{:keys [grocery-id units]}]
       ($ TableContainer {:component Paper}
          ($ Table
             ($ TableHead
                ($ TableRow
                   ($ TableCell "Source")
                   ($ TableCell "Cost")
                   ($ TableCell "Mass")
                   ($ TableCell "Volume")
                   ($ TableCell "Common")
                   ($ TableCell "Actions")))
             ($ TableBody
                (for [u units]
                  ($ grocery-unit-row {:key (:grocery-unit/id u) :grocery-id grocery-id :unit u}))))))

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

         ($ Stack {:direction "column" :spacing 1}
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
                    ($ MenuItem {:value d :key d} (name d)))))
            ($ grocery-unit-table {:grocery-id (:grocery/id grocery)
                                   :units      (:grocery/units grocery)}))))

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
