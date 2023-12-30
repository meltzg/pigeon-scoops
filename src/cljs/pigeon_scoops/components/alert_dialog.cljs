(ns pigeon-scoops.components.alert-dialog
  (:require [uix.core :refer [$ defui]]
            ["@mui/material" :refer [Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogContentText
                                     DialogTitle]]))

(defui alert-dialog [{:keys [open? title message on-close]}]
       ($ Dialog {:open     open?
                  :on-close on-close}
          ($ DialogTitle title)
          ($ DialogContent
             ($ DialogContentText
                ($ :pre {:style {:font-family "inherit"}}
                   message)))
          ($ DialogActions
             ($ Button {:on-click on-close} "Close"))))
