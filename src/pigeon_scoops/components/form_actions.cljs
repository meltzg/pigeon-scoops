(ns pigeon-scoops.components.form-actions
  (:require
   ["antd" :refer [Button Flex]]
   [uix.core :as uix :refer [$ defui]]))

(defui form-actions [{:keys [form entity-id unsaved-changes? on-return on-delete]}]
  ($ Flex {:align "start" :gap "small" :wrap true}
     ($ Button {:html-type "button"
                :disabled unsaved-changes?
                :on-click #(on-return)} "Return to List")
     ($ Button {:type "primary" :html-type "submit" :disabled (and (not (keyword? entity-id)) (not unsaved-changes?))}
        (if (uuid? entity-id) "Update" "Create"))
     ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")
     ($ Button {:html-type "button" :danger true :on-click on-delete} "Delete")))
