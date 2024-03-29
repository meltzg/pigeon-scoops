(ns pigeon-scoops.forms.groceries
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [pigeon-scoops.components.entity-list :refer [entity-list]]
            [pigeon-scoops.spec.groceries :as gs]
            [pigeon-scoops.units.common :as ucom]
            [pigeon-scoops.units.mass :as mass]
            [pigeon-scoops.units.volume :as volume]
            [pigeon-scoops.utils :as utils]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/material" :refer [Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     FormControl
                                     InputLabel
                                     MenuItem
                                     Select
                                     Stack
                                     TextField]]))

(defui unit-config [{:keys [entity on-save on-close]}]
       (let [[source source-valid? on-source-change] (utils/use-validation (or (::gs/source entity) "")
                                                                           #(s/valid? ::gs/source %))
             [mass mass-valid? on-mass-change] (utils/use-validation (or (::gs/unit-mass entity) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (s/valid? ::gs/unit-mass (js/parseFloat %))))
             [mass-type mass-type-valid? on-mass-type-change] (utils/use-validation (or (::gs/unit-mass-type entity) (first (keys mass/conversion-map)))
                                                                                    #(s/valid? ::gs/unit-mass-type %))
             [volume volume-valid? on-volume-change] (utils/use-validation (or (::gs/unit-volume entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::gs/unit-volume (js/parseFloat %))))
             [volume-type volume-type-valid? on-volume-type-change] (utils/use-validation (or (::gs/unit-volume-type entity) (first (keys volume/conversion-map)))
                                                                                          #(s/valid? ::gs/unit-volume-type %))
             [common common-valid? on-common-change] (utils/use-validation (or (::gs/unit-common entity) 0)
                                                                           #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                                 (s/valid? ::gs/unit-common (js/parseFloat %))))
             [common-type common-type-valid? on-common-type-change] (utils/use-validation (or (::gs/unit-common-type entity) (first ucom/other-units))
                                                                                          #(s/valid? ::gs/unit-common-type %))
             [cost cost-valid? on-cost-change] (utils/use-validation (or (::gs/unit-cost entity) 0)
                                                                     #(and (re-matches #"^\d+\.?\d*$" (str %))
                                                                           (s/valid? ::gs/unit-cost (js/parseFloat %))))]
         ($ Dialog {:open true :on-close on-close}
            ($ DialogTitle "Configure Unit")
            ($ DialogContent
               ($ Stack {:direction "column" :spacing 2}
                  ($ TextField {:label     "Source"
                                :value     source
                                :error     (not source-valid?)
                                :on-change on-source-change})
                  ($ TextField {:label     "Mass/Weight"
                                :value     mass
                                :error     (not mass-valid?)
                                :on-change on-mass-change})
                  ($ FormControl {:full-width true
                                  :error      (not mass-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     mass-type
                                :on-change #(on-mass-type-change (keyword (namespace ::mass/kg) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort (keys mass/conversion-map)))))
                  ($ TextField {:label     "Volume"
                                :value     volume
                                :error     (not volume-valid?)
                                :on-change on-volume-change})
                  ($ FormControl {:full-width true
                                  :error      (not volume-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     volume-type
                                :on-change #(on-volume-type-change (keyword (namespace ::volume/c) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort (keys volume/conversion-map)))))
                  ($ TextField {:label     "Common units"
                                :value     common
                                :error     (not common-valid?)
                                :on-change on-common-change})
                  ($ FormControl {:full-width true
                                  :error      (not common-type-valid?)}
                     ($ InputLabel "Unit type")
                     ($ Select {:value     common-type
                                :on-change #(on-common-type-change (keyword (namespace ::ucom/pinch) (.. % -target -value)))}
                        (map #($ MenuItem {:value % :key %} (name %)) (sort ucom/other-units))))
                  ($ TextField {:label     "Cost ($)"
                                :value     cost
                                :error     (not cost-valid?)
                                :on-change on-cost-change})))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save (cond-> {::gs/source    source
                                                       ::gs/unit-cost (js/parseFloat cost)}
                                                      mass-valid? (assoc ::gs/unit-mass (js/parseFloat mass)
                                                                         ::gs/unit-mass-type mass-type)
                                                      volume-valid? (assoc ::gs/unit-volume (js/parseFloat volume)
                                                                           ::gs/unit-volume-type volume-type)
                                                      common-valid? (assoc ::gs/unit-common (js/parseFloat common)
                                                                           ::gs/unit-common-type common-type)))
                          :disabled (not (and source-valid?
                                              cost-valid?
                                              (or (and mass-valid? mass-type-valid?)
                                                  (and volume-valid? volume-type-valid?)
                                                  (and common-valid? common-type-valid?))))}
                  "Save")))))

(defui grocery-entry [{:keys [entry config-metadata set-valid! set-changed-entry! new?]}]
       (let [set-complete-entry! (fn [partial-entry]
                                   (set-changed-entry! (merge (conj {::gs/type  nil
                                                                     ::gs/units []}
                                                                    (when-not (str/blank? (::gs/description entry))
                                                                      [::gs/description (::gs/description entry)]))
                                                              partial-entry)))
             grocery-type-valid? #(and (some? (::gs/type entry))
                                       (re-matches #"^[a-zA-Z0-9-]+$" (name (::gs/type entry))))]
         (uix/use-effect
           (fn []
             (set-valid! (grocery-type-valid?))))

         ($ Stack {:direction "column"
                   :spacing   1}
            ($ TextField {:label     "Type"
                          :error     (not (grocery-type-valid?))
                          :disabled  (not new?)
                          :value     (if (::gs/type entry) (name (::gs/type entry)) "")
                          :on-change #(set-complete-entry!
                                        (assoc entry ::gs/type
                                                     (keyword (namespace ::gs/type) (.. % -target -value))))})
            ($ TextField {:label     "Description"
                          :multiline true
                          :max-rows  4
                          :value     (or (::gs/description entry) "")
                          :on-change #(set-complete-entry! (assoc entry ::gs/description (.. % -target -value)))})
            ($ entity-list {:entity-name    "Unit"
                            :entities       (::gs/units entry)
                            :column-headers ["Source"
                                             "Mass"
                                             "Volume"
                                             "Common Unit"
                                             "Cost"]
                            :cell-text      (for [unit (::gs/units entry)]
                                              [(::gs/source unit)
                                               (str (::gs/unit-mass unit) (when (::gs/unit-mass-type unit)
                                                                            (name (::gs/unit-mass-type unit))))
                                               (str (::gs/unit-volume unit) (when (::gs/unit-volume-type unit)
                                                                              (name (::gs/unit-volume-type unit))))
                                               (str (::gs/unit-common unit) " " (when (::gs/unit-common-type unit)
                                                                                  (name (::gs/unit-common-type unit))))
                                               (str "$" (::gs/unit-cost unit))])
                            :entity-config  unit-config
                            :on-change      #(set-complete-entry! (assoc entry ::gs/units %))}))))
