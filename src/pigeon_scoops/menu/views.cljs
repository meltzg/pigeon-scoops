(ns pigeon-scoops.menu.views
  (:require [pigeon-scoops.context :as ctx]
            [pigeon-scoops.menu.context :as mctx]
            [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.components.number-field :refer [number-field]]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Button
                                     Card
                                     CardActions
                                     CardContent
                                     CardHeader
                                     Collapse
                                     FormControl
                                     IconButton
                                     MenuItem
                                     Select
                                     Stack
                                     Switch
                                     TextField
                                     Typography]]))

(defui menu-card []
       (let [{:keys [menu editable-menu set-editable-menu! unsaved-changes?]} (uix/use-context mctx/menu-context)
             {:constants/keys [menu-durations]} (uix/use-context ctx/constants-context)
             [expanded? set-expanded!] (uix/use-state (or (:menu/active menu)
                                                          (= (:menu/id menu) :new)))]
         (uix/use-effect
           (fn []
             (when menu
               (set-expanded! (:menu/active menu))))
           [menu])
         ($ Card
            ($ CardHeader {:title     (or (:menu/name menu)
                                          "[New Menu]")
                           :subheader (str "Ends on: " (:menu/end-time menu))})
            ($ CardContent
               ($ Collapse {:in expanded?}
                  ($ TextField {:label     "Name"
                                :value     (or (:menu/name editable-menu) "")
                                :on-change #(set-editable-menu! (assoc editable-menu :menu/name (.. % -target -value)))})
                  ($ Stack {:direction "row"}
                     ($ Switch {:checked   (or (:menu/active editable-menu) false)
                                :on-change #(set-editable-menu! (assoc editable-menu :menu/active (.. % -target -checked)))})
                     ($ Typography (if (:menu/active editable-menu) "Active" "Inactive")))
                  ($ Stack {:direction "row"}
                     ($ Switch {:checked   (or (:menu/repeats editable-menu) false)
                                :on-change #(set-editable-menu! (assoc editable-menu :menu/repeats (.. % -target -checked)))})
                     ($ Typography (if (:menu/repeats editable-menu) "Repeats" "Limited Run")))
                  ($ Stack {:direction "row" :spacing 0.5}
                     ($ Typography {:sx (clj->js {:display        "flex"
                                                  :alignItems     "center"
                                                  :justifyContent "center"
                                                  :height         "100%"})}
                        "Duration")
                     ($ number-field {:value          (:menu/duration editable-menu)
                                      :set-value!     #(set-editable-menu! (assoc editable-menu :menu/duration %))
                                      :hide-controls? true})

                     ($ FormControl
                        ($ Select {:value     (or (:menu/duration-type editable-menu) "")
                                   :label     "Unit"
                                   :on-change #(set-editable-menu! (assoc editable-menu
                                                                     :menu/duration-type
                                                                     (->> menu-durations
                                                                          (filter (fn [ut]
                                                                                    (= (name ut)
                                                                                       (.. % -target -value))))
                                                                          (first))))}
                           (for [md menu-durations]
                             ($ MenuItem {:value md :key md} (name md))))))))
            ($ CardActions
               ($ Button {:disabled (not unsaved-changes?)} "Save")
               ($ Button
                  {:disabled (not unsaved-changes?)
                   :on-click (partial set-editable-menu! menu)}
                  "Reset")
               ($ Button "Delete")
               ($ IconButton {:style    {:transform   (str "rotate(" (if expanded? 0 180) "deg)")
                                         :margin-left "auto"}
                              :on-click #(set-expanded! (not expanded?))}
                  ($ ExpandMoreIcon))))))

(defui menu-view []
       (let [{:keys [menus new-menu!]} (uix/use-context mctx/menus-context)]
         ($ Stack {:direction "column"}
            ($ Button {:on-click #(new-menu!)} "New Menu")
            (for [m menus]
              ($ mctx/with-menu
                 {:menu-id (:menu/id m) :key (:menu/id m)}
                 ($ menu-card))))))
