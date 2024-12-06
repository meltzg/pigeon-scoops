(ns pigeon-scoops.forms.orders
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.components.amount-config :refer [amount-config]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.forms.recipes :refer [recipe-entry]]
            [pigeon-scoops.spec.orders :as os]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.utils :as utils :refer [api-url]]
            [goog.string :as gstring]
            [goog.string.format]
            ["@mui/icons-material/MenuBook$default" :as MenuBookIcon]
            ["@mui/material" :refer [Button
                                     Checkbox
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     Divider
                                     FormControl
                                     IconButton
                                     InputLabel
                                     MenuItem
                                     Paper
                                     Select
                                     Stack
                                     TextField
                                     Tooltip
                                     Typography]]))

(defui flavor-config [{:keys [entity config-metadata on-save on-close]}]
       (let [{:keys [flavors]} config-metadata
             [entity set-entity!] (uix/use-state entity)
             set-complete-entity! (fn [partial-entity]
                                    (set-entity! (merge {::os/flavor-id   nil
                                                         ::os/amount      0
                                                         ::os/amount-unit nil}
                                                        partial-entity)))
             [amount-config-valid? set-amount-config-valid!] (uix/use-state false)
             flavor-id-valid? #(and (::os/flavor-id entity)
                                    (s/valid? ::os/flavor-id (::os/flavor-id entity)))]
         (uix/use-effect
           (fn []
             (set-complete-entity! (assoc entity ::os/flavor-id (or (::os/flavor-id entity)
                                                                    (::fs/id (first flavors)))))))

         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Edit Flavor Amount")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ FormControl {:full-width true
                                  :error      (not (flavor-id-valid?))}
                     ($ InputLabel "Flavor")
                     ($ Select {:value     (or (::os/flavor-id entity)
                                               (::fs/id (first flavors)))
                                :on-change #(set-complete-entity!
                                              (assoc entity ::os/flavor-id (uuid (.. % -target -value))))}
                        (map #($ MenuItem {:value (str (::fs/id %)) :key (str (::fs/id %))} (::fs/name %)) (sort ::fs/name flavors))))
                  ($ amount-config {:entry               entity
                                    :on-change           set-complete-entity!
                                    :set-valid!          set-amount-config-valid!
                                    :entry-namespace     (namespace ::os/id)
                                    :default-amount-unit ::volume/c
                                    :accepted-unit-types [::volume/c ::mass/g]})))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save entity)
                          :disabled (not (and (flavor-id-valid?)
                                              amount-config-valid?))}
                  "Save")))))

(defui recipe-viewer [{:keys [flavor-order flavor-data on-close config-metadata]}]
       (let [[recipes set-recipes!] (uix/use-state nil)
             [error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")]
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "flavors/" (str (::os/flavor-id flavor-order)) "/recipes")
                       {:params          {:amount      (::os/amount flavor-order)
                                          :amount-unit (::os/amount-unit flavor-order)}
                        :response-format :transit
                        :handler         set-recipes!
                        :error-handler   (partial utils/error-handler
                                                  set-error-title!
                                                  set-error-text!)}))
           [flavor-order])

         ($ Dialog {:open true :on-close on-close :full-screen true}
            ($ DialogTitle (str "Recipes: " (::fs/name flavor-data)))
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 1.25}
                  ($ alert-dialog {:open?    (not (str/blank? error-title))
                                   :title    error-title
                                   :message  error-text
                                   :on-close #(set-error-title! "")})
                  (for [recipe recipes]
                    [($ Paper {:key (::rs/id recipe)}
                        ($ recipe-entry {:entry recipe :config-metadata config-metadata}))
                     ($ Divider)])))
            ($ DialogActions
               ($ Button {:on-click on-close} "Close")))))

(defn format-unit [unit]
  (str (::gs/unit-purchase-quantity unit)
       ": "
       (str/join " | " (->> (select-keys unit (mapcat #(vec [(keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class %)))
                                                             (keyword (namespace ::gs/unit) (str "unit-" (units/to-unit-class %) "-type"))])
                                                      [::volume/c ::mass/g ::units/pinch]))
                            vals
                            (partition 2)
                            (filter #(some? (first %)))
                            (map #(str (first %) " " (name (second %))))))
       " "
       (::gs/source unit)))

(defui grocery-list-viewer [{:keys [order on-close]}]
       (let [[grocery-data set-grocery-data!] (uix/use-state nil)
             [error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")]
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "orders/" (str (::os/id order)) "/groceries")
                       {:response-format :transit
                        :handler         set-grocery-data!
                        :error-handler   (partial utils/error-handler
                                                  set-error-title!
                                                  set-error-text!)}))
           [order])

         ($ Dialog {:open true :on-close on-close :full-screen true}
            ($ DialogTitle "Groceries")
            ($ DialogContent
               ($ alert-dialog {:open?    (not (str/blank? error-title))
                                :title    error-title
                                :message  error-text
                                :on-close #(set-error-title! "")})
               (when grocery-data
                 ($ Stack {:direction "column" :spacing 1.25}
                    ($ Typography (str "Estimated Total " (gstring/format "$%.2f" (:total-purchase-cost grocery-data))))
                    ($ Typography (str "Estimated Total " (gstring/format "$%.2f" (:total-needed-cost grocery-data))))
                    ($ entity-list {:entity-name    "Groceries"
                                    :entities       (:purchase-list grocery-data)
                                    :column-headers ["Item"
                                                     "Amount Needed"
                                                     "Amount Cost"
                                                     "Purchase Quantity"
                                                     "Purchase Units"
                                                     "Purchase Cost"]
                                    :cell-text      (for [item (sort-by ::gs/type (:purchase-list grocery-data))]
                                                      [(name (::gs/type item))
                                                       (str (gstring/format "%.4f" (::gs/amount-needed item)) " " (name (::gs/amount-needed-unit item)))
                                                       (gstring/format "$%.2f" (or (::gs/amount-needed-cost item) 0))
                                                       (when (::gs/purchase-amount item)
                                                         (str (gstring/format "%.4f" (::gs/purchase-amount item)) " " (name (::gs/purchase-amount-unit item))))
                                                       (str/join "\n" (map format-unit (::gs/units item)))
                                                       (gstring/format "$%.2f" (or (::gs/purchase-cost item) 0))])
                                    :cell-action    (repeat (count (:purchase-list grocery-data))
                                                            ($ Checkbox))
                                    :frozen?        true}))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Close")))))

(defui order-entry [{:keys [entry config-metadata set-valid! set-changed-entry! unsaved? new?]}]
       (let [order-id (::os/id entry)
             set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::os/note    ""
                                                                     ::os/flavors []}
                                                                    (when (some? order-id) [::os/id order-id]))
                                                              partial-entry)))
             note-valid? #(s/valid? ::os/note (::os/note entry))
             [displayed-flavor set-displayed-flavor!] (uix/use-state nil)
             [show-grocery-list? set-show-grocery-list!] (uix/use-state false)]
         (uix/use-effect
           (fn []
             (set-valid! (note-valid?))))

         ($ Stack {:direction "column"
                   :spacing   1.25}
            (when-not (nil? displayed-flavor)
              ($ recipe-viewer {:flavor-order    displayed-flavor
                                :flavor-data     (first (filter #(= (::os/flavor-id displayed-flavor) (::fs/id %))
                                                                (:flavors config-metadata)))
                                :config-metadata config-metadata
                                :on-close        (partial set-displayed-flavor! nil)}))
            (when show-grocery-list?
              ($ grocery-list-viewer {:order    entry
                                      :on-close (partial set-show-grocery-list! false)}))
            ($ TextField {:label     "Note"
                          :error     (not (note-valid?))
                          :value     (or (::os/note entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::os/note (.. % -target -value)))})
            ($ entity-list {:entity-name     "Flavors"
                            :entities        (::os/flavors entry)
                            :column-headers  ["Name" "Amount"]
                            :cell-text       (for [flavor (::os/flavors entry)]
                                               [(::fs/name (first (filter #(= (::fs/id %)
                                                                              (::os/flavor-id flavor))
                                                                          (:flavors config-metadata))))
                                                (str (::os/amount flavor)
                                                     " "
                                                     (name (::os/amount-unit flavor)))])
                            :cell-action     (for [flavor (::os/flavors entry)]
                                               [($ Tooltip {:title "Show Recipes" :key (str (::os/flavor-id flavor) "show-recipe")}
                                                   ($ IconButton {:on-click (partial set-displayed-flavor! flavor)}
                                                      ($ MenuBookIcon)))])
                            :config-metadata config-metadata
                            :entity-config   flavor-config
                            :on-change       #(set-complete-entry! (assoc entry ::os/flavors %))})
            (when-not new?
              ($ Button {:variant "contained" :disabled unsaved? :on-click (partial set-show-grocery-list! true)}
                 "View Grocery List")))))
