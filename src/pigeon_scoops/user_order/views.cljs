(ns pigeon-scoops.user-order.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table]]
   [clojure.string :as str]
   [pigeon-scoops.user-order.forms :refer [order-form]]
   [pigeon-scoops.hooks :refer [use-orders]]
   [pigeon-scoops.utils :refer [make-sorter stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui order-view [{:keys [path]}]
  (let [{:keys [order-id]} path]
    ($ order-form {:order-id order-id})))

(def columns
  [{:title "Note"
    :dataIndex (stringify-keyword :user-order/note)
    :sorter (make-sorter :user-order/note)
    :key :name}
   {:title "Department"
    :dataIndex (stringify-keyword :user-order/status)
    :render (fn [val]
              (str/capitalize (name (keyword val))))
    :key :public}
   {:title ($ Space
              "Actions"
              ($ Button {:type "text"
                         :icon ($ FileAddOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.user-order.routes/order
                                     {:order-id :new})}))
    :render (fn [_ order]
              ($ Button {:type "text"
                         :icon ($ ExportOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.user-order.routes/order
                                     {:order-id (:user-order/id (js->clj order :keywordize-keys true))})}))}])

(defui orders-table []
  (let [{:keys [orders loading?]} (use-orders)]
    (if loading?
      ($ Spin)
      ($ Table {:columns (clj->js columns)
                :dataSource (clj->js (map-indexed (fn [idx order] (assoc order :key idx))
                                                  (sort-by (comp str/lower-case :user-order/note) orders))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
