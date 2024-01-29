(ns pigeon-scoops.forms.orders
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.components.amount-config :refer [amount-config]]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [pigeon-scoops.spec.orders :as os]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.utils :as utils :refer [api-url]]
            ["@mui/icons-material/MenuBook$default" :as MenuBookIcon]
            ["@mui/material" :refer [Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     FormControl
                                     IconButton
                                     InputLabel
                                     MenuItem
                                     Paper
                                     Select
                                     Stack
                                     TextField
                                     Tooltip]]))

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
                        (map #($ MenuItem {:value (str (::fs/id %)) :key (str (::fs/id %))} (::fs/name %)) flavors)))
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

(defui recipe-viewer [{:keys [flavor-order flavor-data on-close]}]
       (let [[recipes set-recipes!] (uix/use-state nil)
             [error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         (uix/use-effect
           (fn []
             (ajax/GET (str api-url "flavors/" (str (::os/flavor-id flavor-order)) "/recipes")
                       {:params          {:amount      (::os/amount flavor-order)
                                          :amount-unit (::os/amount-unit flavor-order)}
                        :format          :transit
                        :response-format :transit
                        :handler         set-recipes!}))
           [flavor-order error-handler])
         ($ Dialog {:open true :on-close on-close :full-screen true}
            ($ DialogTitle (str "Recipes: " (::fs/name flavor-data)))
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 1.25}
                  ($ alert-dialog {:open?    (not (str/blank? error-title))
                                   :title    error-title
                                   :message  error-text
                                   :on-close #(set-error-title! "")})
                  (for [recipe recipes]
                    ($ Paper {:key (::rs/id recipe)}
                       ($ :pre {:style {:font-family "inherit"}}
                          recipe)))))
            ($ DialogActions
               ($ Button {:on-click on-close} "Close")))))

(defui order-entry [{:keys [entry config-metadata set-valid! set-changed-entry!]}]
       (let [order-id (::os/id entry)
             set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::os/note    ""
                                                                     ::os/flavors []}
                                                                    (when (some? order-id) [::os/id order-id]))
                                                              partial-entry)))
             note-valid? #(s/valid? ::os/note (::os/note entry))
             [displayed-flavor set-displayed-flavor!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (set-valid! (note-valid?))))

         ($ Stack {:direction "column"
                   :spacing   1.25}
            (when-not (nil? displayed-flavor)
              ($ recipe-viewer {:flavor-order displayed-flavor
                                :flavor-data  (first (filter #(= (::os/flavor-id displayed-flavor) (::fs/id %))
                                                             (:flavors config-metadata)))
                                :on-close     (partial set-displayed-flavor! nil)}))
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
                            :on-change       #(set-complete-entry! (assoc entry ::os/flavors %))}))))
