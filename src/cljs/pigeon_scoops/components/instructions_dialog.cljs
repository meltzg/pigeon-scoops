(ns pigeon-scoops.components.instructions-dialog
  (:require [uix.core :as uix :refer [$ defui]]
            [pigeon-scoops.utils :as utils]
            [clojure.string :as str]
            ["@mui/material" :refer [Button
                                     Dialog
                                     DialogActions
                                     DialogContent
                                     DialogTitle
                                     TextField]]))

(defui instructions-dialog [{:keys [instructions validate-fn on-save on-close]}]
       (let [convert-instructions #(remove str/blank? (str/split-lines %))
             [new-instructions new-instructions-valid? on-new-instructions-change] (utils/use-validation (str/join "\n" instructions)
                                                                                                         (comp validate-fn convert-instructions))]
         ($ Dialog {:open true :on-close on-close :full-screen true}
            ($ DialogTitle "Edit Instructions")
            ($ DialogContent
               ($ TextField {:label      "Instructions"
                             :multiline  true
                             :full-width true
                             :value      new-instructions
                             :on-change  on-new-instructions-change}))
            ($ DialogActions
               ($ Button {:on-click on-close} "Cancel")
               ($ Button {:on-click #(on-save (convert-instructions new-instructions))
                          :disabled (not new-instructions-valid?)}
                  "Save")))))
