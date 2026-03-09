(ns pigeon-scoops.grocery.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table]]
   [clojure.string :as str]
   [pigeon-scoops.grocery.forms :refer [grocery-form]]
   [pigeon-scoops.hooks :refer [use-groceries]]
   [pigeon-scoops.utils :refer [make-sorter stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui grocery-view [{:keys [path]}]
  (let [{:keys [grocery-id]} path]
    ($ grocery-form {:grocery-id grocery-id})))

(def columns
  [{:title "Name"
    :dataIndex (stringify-keyword :grocery/name)
    :sorter (make-sorter :grocery/name)
    :key :name}
   {:title "Department"
    :dataIndex (stringify-keyword :grocery/department)
    :render (fn [val]
              (str/capitalize (name (keyword val))))
    :key :public}
   {:title ($ Space
              "Actions"
              ($ Button {:type "text"
                         :icon ($ FileAddOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.grocery.routes/grocery
                                     {:grocery-id :new})}))
    :render (fn [_ grocery]
              ($ Button {:type "text"
                         :icon ($ ExportOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.grocery.routes/grocery
                                     {:grocery-id (:grocery/id (js->clj grocery :keywordize-keys true))})}))}])

(defui groceries-table []
  (let [{:keys [groceries loading?]} (use-groceries)]
    (if loading?
      ($ Spin)
      ($ Table {:columns (clj->js columns)
                :dataSource (clj->js (map-indexed (fn [idx grocery] (assoc grocery :key idx))
                                                  (sort-by (comp str/lower-case :grocery/name) groceries))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
