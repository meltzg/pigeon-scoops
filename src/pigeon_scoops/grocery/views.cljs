(ns pigeon-scoops.grocery.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table Tag]]
   [clojure.string :as str]
   [pigeon-scoops.grocery.forms :refer [grocery-form]]
   [pigeon-scoops.hooks :refer [use-groceries]]
   [pigeon-scoops.utils :refer [make-sorter stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui grocery-view [{:keys [path]}]
  (let [{:keys [grocery-id]} path]
    ($ grocery-form {:grocery-id grocery-id})))

(defn make-columns [data]
  [{:title "Name"
    :dataIndex (stringify-keyword :grocery/name)
    :sorter (make-sorter :grocery/name)
    :filterSearch true
    :filters (->> data
                  (map :grocery/name)
                  (filter some?)
                  (set)
                  (sort)
                  (map (fn [name] {:text name
                                   :value name})))
    :onFilter (fn [value record]
                (str/includes? (str/lower-case (:grocery/name (js->clj record :keywordize-keys true)))
                               (str/lower-case value)))
    :key :name}
   {:title "Department"
    :dataIndex (stringify-keyword :grocery/department)
    :render (fn [val]
              (let [val (keyword val)]
                ($ Tag {:color (case val
                                 :department/produce "green"
                                 :department/dairy "blue"
                                 :department/meat "red"
                                 :department/bakery "orange"
                                 :department/grocery "gray"
                                 "gray")}
                   (str/capitalize (name val)))))
    :filterSearch true
    :filters (->> data
                  (map :grocery/department)
                  (filter some?)
                  (set)
                  (sort)
                  (map (fn [department] {:text (str/capitalize (name (keyword department)))
                                         :value department})))
    :onFilter (fn [value record]
                (= value (name (keyword (:grocery/department (js->clj record :keywordize-keys true))))))
    :key :department}
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
      ($ Table {:columns (clj->js (make-columns groceries))
                :dataSource (clj->js (map-indexed (fn [idx grocery] (assoc grocery :key idx))
                                                  (sort-by (comp str/lower-case :grocery/name) groceries))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
