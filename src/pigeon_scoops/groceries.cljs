(ns pigeon-scoops.groceries
  (:require [clojure.string :as str]
            [pigeon-scoops.api :as api]
            [pigeon-scoops.components.number-field :refer [number-field]]
            [pigeon-scoops.context :as ctx]
            [pigeon-scoops.hooks :refer [use-token]]
            [reitit.frontend.easy :as rfe]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
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

(def grocery-context (uix/create-context))

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
             reset! (uix/use-memo #(fn [g]
                                     (set-name! (or (:grocery/name g) ""))
                                     (set-department! (or (:grocery/department g) ""))
                                     (set-units! (:grocery/units g)))
                                  [])
             [refresh? set-refresh!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (when (and grocery-id token)
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
                                                  :unsaved-changes? unsaved-changes?
                                                  :reset!           reset!
                                                  :refresh!         #(set-refresh! (not refresh?))}}
            children)))

(defui grocery-list [{:keys [selected-grocery-id]}]
       (let [{:keys [groceries]} (uix/use-context ctx/groceries-context)
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
             {:keys [set-unit! remove-unit!]} (uix/use-context grocery-context)]
         ($ TableRow
            ($ TableCell
               ($ TextField {:value     (:grocery-unit/source unit)
                             :on-change #(set-unit! (assoc unit :grocery-unit/source (.. % -target -value)))}))
            ($ TableCell
               ($ number-field {:value (:grocery-unit/unit-cost unit) :set-value! #(set-unit! (assoc unit :grocery-unit/unit-cost %))}))
            (for [[idx [value-key type-key option-key]]
                  (map-indexed vector [[:grocery-unit/unit-mass :grocery-unit/unit-mass-type :mass]
                                       [:grocery-unit/unit-volume :grocery-unit/unit-volume-type :volume]
                                       [:grocery-unit/unit-common :grocery-unit/unit-common-type :common]])]

              ($ TableCell {:key idx}
                 ($ Stack {:direction "row" :spacing 1}
                    ($ number-field {:value (value-key unit) :set-value! #(set-unit! (assoc unit value-key %))})
                    ($ FormControl
                       ($ Select {:value     (or (type-key unit) "")
                                  :on-change #(set-unit! (assoc unit type-key (keyword (name option-key) (.. % -target -value))))}
                          (for [o (option-key unit-types)]
                            ($ MenuItem {:value o :key o} (name o))))))))
            ($ TableCell
               ($ IconButton {:color    "error"
                              :on-click (partial remove-unit! (:grocery-unit/id unit))}
                  ($ DeleteIcon))))))



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

(defui grocery-control []
       (let [{:constants/keys [departments]} (uix/use-context ctx/constants-context)
             {:keys [grocery grocery-name set-name! department set-department! units reset! unsaved-changes?]} (uix/use-context grocery-context)
             department-label-id (str "department-" (:grocery/id grocery))]

         (uix/use-effect
           (fn []
             (when grocery
               (reset! grocery)))
           [grocery reset!])

         ($ Stack {:direction "column" :spacing 1}
            ($ Button {:on-click #(rfe/push-state :pigeon-scoops.routes/groceries)}
               "Back to list")
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
                                   :units      units})
            ($ Stack {:direction "row" :spacing 1}
               ($ Button {:variant "contained" :disabled (not unsaved-changes?)} "Save")
               ($ Button {:variant  "contained"
                          :on-click (partial reset! grocery)
                          :disabled (not unsaved-changes?)}
                  "Reset")))))

(defui grocery-view [{:keys [path]}]
       (let [{:keys [grocery-id]} path]
         ($ with-grocery {:grocery-id grocery-id}
            ($ Stack {:direction "row" :spacing 1}
               ($ grocery-list {:selected-grocery-id grocery-id})
               ($ grocery-control)))))

(defui grocery-row [{:keys [grocery]}]
       ($ TableRow
          ($ TableCell {:on-click #(rfe/push-state :pigeon-scoops.routes/grocery {:grocery-id (:grocery/id grocery)})}
             (:grocery/name grocery))
          ($ TableCell
             (name (:grocery/department grocery)))
          ($ TableCell
             ($ IconButton {:color    "error"
                            :on-click #(prn "delete" (:grocery/id grocery))}
                ($ DeleteIcon)))))


(defui groceries-table []
       (let [{:keys [groceries]} (uix/use-context ctx/groceries-context)]
         ($ TableContainer {:sx (clj->js {:maxHeight "calc(100vh - 75px)"
                                          :overflow  "auto"})}
            ($ Table {:sticky-header true}
               ($ TableHead
                  ($ TableRow
                     ($ TableCell "Name")
                     ($ TableCell "Department")
                     ($ TableCell "Actions")))
               ($ TableBody
                  (for [g (sort-by :grocery/name groceries)]
                    ($ grocery-row {:key (:grocery/id g) :grocery g})))))))
