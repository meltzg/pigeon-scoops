(ns pigeon-scoops.components.status-tag
  (:require
   [antd :refer [Tag]]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defui status-tag [{:keys [status]}]
  ($ Tag {:color (case status
                   :status/draft "orange"
                   :status/submitted "blue"
                   :status/in-progress "purple"
                   :status/complete "green"
                   "gray")}
     (str/capitalize (name status))))
