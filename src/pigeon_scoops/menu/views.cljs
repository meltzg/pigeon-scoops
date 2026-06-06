(ns pigeon-scoops.menu.views
  (:require
   ["@ant-design/icons" :refer [FileAddOutlined]]
   ["react-icons/md" :refer [MdDeleteForever]]
   [antd :refer [Button Space Spin Table Tag]]
   [clojure.string :as str]
   [pigeon-scoops.fetchers :refer [delete-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-menus! use-menus use-token]]
   [pigeon-scoops.menu.forms :refer [menu-form]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :refer [$ defui] :as uix]))

(defui menu-view [{:keys [path]}]
  (let [{:keys [menu-id]} path]
    ($ menu-form {:menu-id menu-id})))

(defn make-columns [token]
  [(merge {:title "Name"
           :dataIndex (stringify-keyword :menu/name)
           :render (fn [_ record]
                     (let [record (js->clj record :keywordize-keys true)]
                       ($ :a {:href (rfe/href :pigeon-scoops.menu.routes/menu {:menu-id (:menu/id record)})}
                          (:menu/name record))))
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
                                     {:menu-id :new})}
                 "New Menu"))
    :render (fn [_ menu]
              ($ Button {:type "text"
                         :icon ($ MdDeleteForever {:size 25})
                         :danger true
                         :on-click #(-> menu
                                        (js->clj :keywordize-keys true)
                                        :menu/id
                                        ((fn [menu-id]
                                           (-> (delete-fetcher! (str base-url "/menus/" menu-id)
                                                                {:token token})
                                               (.then (fn []
                                                        (invalidate-menus!)))))))}))}])

(defui menu-table []
  (let [{:keys [menus loading?]} (use-menus true false)
        {:keys [token] token-loading? :loading?} (use-token)]
    (if (or loading? token-loading?)
      ($ Spin)
      ($ Table {:columns (clj->js (make-columns token))
                :dataSource (clj->js (map-indexed (fn [idx menu] (assoc menu :key idx))
                                                  (sort-by #(str/lower-case (:menu/name %)) menus))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
