(ns pigeon-scoops.utils.table
  (:require
   [uix.core :as uix :refer [$]]
   [clojure.string :as str]
   ["react-icons/fa" :refer [FaSearch]]
   [antd :refer [Button Input Space]]))

(defn make-sorter [key]
  (fn [a b]
    (let [[a b] (map #(js->clj % :keywordize-keys true) [a b])]
      (compare (get a key) (get b key)))))

(defn make-filter [key]
  {:filterDropdown (fn [props]
                     (let [{:strs [selectedKeys setSelectedKeys confirm clearFilters]}
                           (js->clj props)
                           selected-value (or (first selectedKeys) "")]
                       ($ Space {:orientation "vertical"
                                 :style {:padding 8}}
                          ($ Input {:placeholder (str "Filter by " (name key))
                                    :value selected-value
                                    :allowClear true
                                    :on-change (fn [e]
                                                 (let [value (.. e -target -value)]
                                                   (if (seq value)
                                                     (setSelectedKeys (clj->js [value]))
                                                     (clearFilters))
                                                   (confirm #js {:closeDropdown false})))
                                    :on-press-enter #(confirm)})
                          ($ Space
                             ($ Button {:type "primary"
                                        :on-click #(confirm)}
                                "Search")
                             ($ Button {:on-click #(do
                                                     (clearFilters)
                                                     (confirm))}
                                "Reset")))))
   :filterIcon (fn [filtered?]
                 ($ FaSearch {:style {:color (when filtered? "#1890ff")}}))
   :onFilter (fn [value record]
               (let [name (some-> (key (js->clj record :keywordize-keys true))
                                  str/lower-case)]
                 (and name
                      (str/includes? name (str/lower-case (str value))))))})
