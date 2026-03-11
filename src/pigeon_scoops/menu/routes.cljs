(ns pigeon-scoops.menu.routes
  (:require [cljs.spec.alpha :as s]
            [pigeon-scoops.menu.views :refer [menu-table menu-view]]))

(def routes ["/menu"
             ["" {:name ::menus
                  :view menu-table}]
             ["/:menu-id" {:name       ::menu
                           :view       menu-view
                           :parameters {:path {:menu-id (s/or :uuid uuid? :key keyword?)}}}]])
