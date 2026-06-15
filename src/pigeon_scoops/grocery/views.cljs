(ns pigeon-scoops.grocery.views
  (:require
   ["@ant-design/icons" :refer [FileAddOutlined]]
   ["react-icons/md" :refer [MdDeleteForever]]
   [antd :refer [Button Space Spin Table Tag]]
   [clojure.string :as str]
   [pigeon-scoops.fetchers :refer [delete-fetcher!]]
   [pigeon-scoops.grocery.forms :refer [grocery-form]]
   [pigeon-scoops.hooks :refer [base-url invalidate-groceries! use-groceries
                                use-token]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui grocery-view [{:keys [path]}]
  (let [{:keys [grocery-id]} path]
    ($ grocery-form {:grocery-id grocery-id})))

(defn make-columns [data token]
  [(merge {:title "Name"
           :dataIndex (stringify-keyword :grocery/name)
           :render (fn [_ record]
                     (let [record (js->clj record :keywordize-keys true)]
                       ($ :a {:href (rfe/href :pigeon-scoops.grocery.routes/grocery {:grocery-id (:grocery/id record)})}
                          (:grocery/name record))))
           :sorter (make-sorter :grocery/name)
           :key :name}
          (make-filter :grocery/name))
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
                                     {:grocery-id :new})}
                 "New Grocery"))
    :render (fn [_ grocery]
              ($ Button {:type "text"
                         :icon ($ MdDeleteForever {:size 25})
                         :danger true
                         :on-click #(-> grocery
                                        (js->clj :keywordize-keys true)
                                        :grocery/id
                                        ((fn [grocery-id]
                                           (-> (delete-fetcher! (str base-url "/groceries/" grocery-id)
                                                                {:token token})
                                               (.then (fn []
                                                        (invalidate-groceries!)))))))}))}])

(defui groceries-table []
  (let [{:keys [groceries loading?]} (use-groceries)
        {:keys [token] token-loading? :loading?} (use-token)]
    (if (or loading? token-loading?)
      ($ Spin)
      ($ Table {:columns (clj->js (make-columns groceries token))
                :dataSource (clj->js (map-indexed (fn [idx grocery] (assoc grocery :key idx))
                                                  (sort-by (comp str/lower-case :grocery/name) groceries))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
