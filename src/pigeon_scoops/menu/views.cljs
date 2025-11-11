(ns pigeon-scoops.menu.views
  (:require [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.menu.context :as mctx]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Button
                                     Card
                                     CardActions
                                     CardContent
                                     CardHeader
                                     Collapse
                                     IconButton
                                     Stack]]))

(defui menu-card [{:keys [menu]}]
       (let [[expanded? set-expanded!] (uix/use-state (or (:menu/active menu)
                                                          (= (:menu/id menu) :new)))]
         ($ Card
            ($ CardHeader {:title (or (:menu/name menu)
                                      "[New Menu]")
                           :subheader (str "Ends on: "(:menu/end-time menu))})
            ($ CardContent
               ($ Collapse {:in expanded?}
                  "asdf"))
            ($ CardActions
               ($ Button "Save")
               ($ Button "Reset")
               ($ Button "Delete")
               ($ IconButton {:style {:transform (str "rotate(" (if expanded? 0 180) "deg)")
                                      :margin-left "auto"}
                              :on-click #(set-expanded! (not expanded?))}
                  ($ ExpandMoreIcon))))))

(defui menu-view []
       (let [{:keys [menus new-menu!]} (uix/use-context mctx/menus-context)]
         ($ Stack {:direction "column"}
            ($ Button {:on-click #(new-menu!)} "New Menu")
            (for [m menus]
              ($ menu-card {:menu m :key (:menu/id m)})))))
