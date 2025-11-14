(ns pigeon-scoops.menu.views
  (:require [pigeon-scoops.menu.context :as mctx]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Button
                                     Card
                                     CardActions
                                     CardContent
                                     CardHeader
                                     Collapse
                                     IconButton
                                     Stack]]))

(defui menu-card []
       (let [{:keys [menu]} (uix/use-context mctx/menu-context)
             [expanded? set-expanded!] (uix/use-state (or (:menu/active menu)
                                                          (= (:menu/id menu) :new)))]
         (uix/use-effect
           (fn []
             (set-expanded! (or expanded? (:menu/active menu))))
           [expanded? menu])
         ($ Card
            ($ CardHeader {:title     (or (:menu/name menu)
                                          "[New Menu]")
                           :subheader (str "Ends on: " (:menu/end-time menu))})
            ($ CardContent
               ($ Collapse {:in expanded?}
                  (str "active " (:menu/active menu))))
            ($ CardActions
               ($ Button "Save")
               ($ Button "Reset")
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
