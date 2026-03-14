(ns pigeon-scoops.user-order.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table Tag]]
   [clojure.string :as str]
   [pigeon-scoops.hooks :refer [use-orders]]
   [pigeon-scoops.user-order.forms :refer [order-form]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui order-view [{:keys [path]}]
  (let [{:keys [order-id]} path]
    ($ order-form {:order-id order-id})))

(defn make-columns [data]
  [(merge {:title "Note"
           :dataIndex (stringify-keyword :user-order/note)
           :sorter (make-sorter :user-order/note)
           :key :name}
          (make-filter :user-order/note))
   {:title "Status"
    :dataIndex (stringify-keyword :user-order/status)
    :render (fn [val]
              (let [val (keyword val)]
                ($ Tag {:color (case val
                                 :status/draft "orange"
                                 :status/submitted "blue"
                                 :status/in-progress "purple"
                                 :status/complete "green"
                                 "gray")}
                   (str/capitalize (name val)))))
    :filters (->> data
                  (map :user-order/status)
                  (filter some?)
                  (set)
                  (sort)
                  (map (fn [status] {:text (str/capitalize (name (keyword status)))
                                     :value status})))
    :onFilter (fn [value record]
                (= value (name (keyword (:user-order/status (js->clj record :keywordize-keys true))))))
    :key :status}
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
      ($ Table {:columns (clj->js (make-columns orders))
                :dataSource (clj->js (map-indexed (fn [idx order] (assoc order :key idx))
                                                  (sort-by (comp str/lower-case :user-order/note) orders))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
