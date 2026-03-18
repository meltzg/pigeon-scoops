(ns pigeon-scoops.menu.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table Tag]]
   [clojure.string :as str]
   [pigeon-scoops.hooks :refer [use-menus]]
   [pigeon-scoops.menu.forms :refer [menu-form]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [reitit.frontend.easy :as rfe]
   [uix.core :refer [$ defui] :as uix]))

(defui menu-view [{:keys [path]}]
  (let [{:keys [menu-id]} path]
    ($ menu-form {:menu-id menu-id})))

(def columns
  [(merge {:title "Name"
           :dataIndex (stringify-keyword :menu/name)
           :sorter (make-sorter :menu/name)
           :key :name}
          (make-filter :menu/name))
   {:title "Active"
    :dataIndex (stringify-keyword :menu/active)
    :render #(if %
               ($ Tag {:color "green"}
                  "Yes")
               ($ Tag {:color "red"}
                  "No"))
    :key :active}
   {:title "Repeats"
    :dataIndex (stringify-keyword :menu/repeats)
    :render #(if %
               ($ Tag {:color "green"}
                  "Yes")
               ($ Tag {:color "red"}
                  "No"))
    :key :repeats}
   {:title "Duration"
    :dataIndex (stringify-keyword :menu/duration)
    :render (fn [duration menu]
              (str duration " " (-> menu
                                    (js->clj :keywordize-keys true)
                                    :menu/duration-type
                                    (parse-keyword)
                                    (name))))
    :key :duration}
   {:title "End Time"
    :dataIndex (stringify-keyword :menu/end-time)
    :sorter (make-sorter :menu/end-time)
    :render #(when % (.toLocaleString %))
    :key :end-time}
   {:title ($ Space
              "Actions"
              ($ Button {:type "text"
                         :icon ($ FileAddOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.menu.routes/menu
                                     {:menu-id :new})}))
    :render (fn [_ menu]
              ($ Button {:type "text"
                         :icon ($ ExportOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.menu.routes/menu
                                     {:menu-id (:menu/id (js->clj menu :keywordize-keys true))})}))}])

(defui menu-table []
  (let [{:keys [menus loading?]} (use-menus)]
    (if loading?
      ($ Spin)
      ($ Table {:columns (clj->js columns)
                :dataSource (clj->js (map-indexed (fn [idx menu] (assoc menu :key idx))
                                                  (sort-by #(str/lower-case (:menu/name %)) menus))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
