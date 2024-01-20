(ns pigeon-scoops.apps.auth
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/AccountCircle$default" :as AccountCircle]
            ["@mui/material" :refer [Button
                                     IconButton
                                     Menu
                                     MenuItem
                                     Stack]]))

(defui authenticator [{:keys [signed-in? on-change]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [anchor-el set-anchor-el!] (uix/use-state nil)
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column"}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            ($ IconButton {:size     "large"
                           :on-click #(set-anchor-el! (.-currentTarget %))
                           :color    (if signed-in? "default" "error")}
               ($ AccountCircle))
            ($ Menu {:id               "menu-connection"
                     :anchor-el        anchor-el
                     :anchor-origin    {:vertical "top" :horizontal "right"}
                     :keep-mounted     true
                     :transform-origin {:vertical "top" :horizontal "right"}
                     :open             (some? anchor-el)
                     :on-close         #(set-anchor-el! nil)}
               ($ MenuItem {:on-click (partial handle-refresh on-change)}
                  "Refresh Status")
               (if signed-in?)))))
