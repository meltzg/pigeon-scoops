(ns pigeon-scoops.apps.auth
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [pigeon-scoops.utils :as utils]
            [pigeon-scoops.components.alert-dialog :refer [alert-dialog]]
            [uix.core :as uix :refer [$ defui]]
            ["@mui/icons-material/AccountCircle$default" :as AccountCircle]
            ["@mui/material" :refer [Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     IconButton
                                     Menu
                                     MenuItem
                                     Stack
                                     TextField]]))

(defui authenticator [{:keys [signed-in? on-change]}]
       (let [[error-text set-error-text!] (uix/use-state "")
             [error-title set-error-title!] (uix/use-state "")
             [anchor-el set-anchor-el!] (uix/use-state nil)
             [sign-in-open? set-sign-in-open!] (uix/use-state false)
             [email email-valid? on-email-change] (utils/use-validation "" (comp not str/blank?))
             [password password-valid? on-password-change] (utils/use-validation "" (comp not str/blank?))
             error-handler (partial utils/error-handler
                                    set-error-title!
                                    set-error-text!)]
         ($ Stack {:direction "column"}
            ($ alert-dialog {:open?    (not (str/blank? error-title))
                             :title    error-title
                             :message  error-text
                             :on-close #(set-error-title! "")})
            ($ Dialog {:open     sign-in-open?
                       :on-close (partial set-sign-in-open! false)}
               ($ DialogTitle "Sign In")
               ($ DialogContent
                  ($ Stack {:direction "column"
                            :spacing   2}
                     ($ TextField {:label     "Email"
                                   :value     email
                                   :error     (not email-valid?)
                                   :on-change on-email-change})
                     ($ TextField {:label     "Password"
                                   :type      "password"
                                   :value     password
                                   :error     (not password-valid?)
                                   :on-change on-password-change})))
               ($ DialogActions
                  ($ Button {:on-click (partial set-sign-in-open! false)} "Cancel")
                  ($ Button {:disabled (not (and email-valid? password-valid?))
                             :on-click #(ajax/POST (str utils/api-url "signIn")
                                                   {:params        {:email email :password password}
                                                    :format        :transit
                                                    :handler       (fn []
                                                                     (on-change true)
                                                                     (set-sign-in-open! false)
                                                                     (set-anchor-el! nil))
                                                    :error-handler error-handler})}
                     "Submit")))
            ($ IconButton {:size     "large"
                           :on-click #(set-anchor-el! (.-currentTarget %))
                           :color    (if signed-in? "default" "error")}
               ($ AccountCircle))
            ($ Menu {:id           "menu-connection"
                     :anchor-el    anchor-el
                     :keep-mounted true
                     :open         (some? anchor-el)
                     :on-close     #(set-anchor-el! nil)}
               ($ MenuItem {:on-click #(ajax/GET (str utils/api-url "signIn")
                                                 {:handler       (comp (partial on-change true)
                                                                       (partial set-anchor-el! nil))
                                                  :error-handler error-handler})}
                  "Refresh Status")
               (if signed-in?
                 ($ MenuItem {:on-click #(ajax/POST (str utils/api-url "signOut")
                                                    {:handler       (comp (partial on-change false)
                                                                          (partial set-anchor-el! nil))
                                                     :error-handler error-handler})}
                    "Sign Out")
                 ($ MenuItem {:on-click (partial set-sign-in-open! true)}
                    "Sign In"))))))
