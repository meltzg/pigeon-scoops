(ns pigeon-scoops.recipe.views
  (:require
   [antd :refer [Button Space Spin Table Tag]]
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [pigeon-scoops.hooks :refer [use-recipes]]
   [pigeon-scoops.recipe.forms :refer [recipe-form]]
   [pigeon-scoops.utils :refer [make-sorter stringify-keyword]]
   [uix.core :as uix :refer [$ defui]]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as str]))

(defui recipe-view [{:keys [path query]}]
  (let [{:keys [recipe-id]} path
        {:keys [amount amount-unit]} query]
    ($ recipe-form {:recipe-id recipe-id
                    :scaled-amount amount
                    :scaled-amount-unit amount-unit})))

(defn make-columns [data]
  [{:title "Name"
    :dataIndex (stringify-keyword :recipe/name)
    :sorter (make-sorter :recipe/name)
    :filterSearch true
    :filters (->> data
                  (map :recipe/name)
                  (filter some?)
                  (set)
                  (sort)
                  (map (fn [name] {:text name
                                   :value name})))
    :onFilter (fn [value record]
                (str/includes? (str/lower-case (:recipe/name (js->clj record :keywordize-keys true)))
                               (str/lower-case value)))
    :key :name}
   {:title "Public"
    :dataIndex (stringify-keyword :recipe/public)
    :render #(if %
               ($ Tag {:color "green"}
                  "Yes")
               ($ Tag {:color "red"}
                  "No"))
    :filters [{:text "Yes" :value true}
              {:text "No" :value false}]
    :onFilter (fn [value record]
                (= value (:recipe/public (js->clj record :keywordize-keys true))))
    :key :public}
   {:title ($ Space
              "Actions"
              ($ Button {:type "text"
                         :icon ($ FileAddOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.recipe.routes/recipe
                                     {:recipe-id :new})}))
    :render (fn [_ recipe]
              ($ Button {:type "text"
                         :icon ($ ExportOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.recipe.routes/recipe
                                     {:recipe-id (:recipe/id (js->clj recipe :keywordize-keys true))})}))}])

(defui recipes-table []
  (let [{:keys [recipes loading?]} (use-recipes)]
    (if loading?
      ($ Spin)
      ($ Table {:columns (clj->js (make-columns (apply concat (vals recipes))))
                :dataSource (clj->js (map-indexed (fn [idx recipe] (assoc recipe :key idx))
                                                  (sort-by #(str/lower-case (:recipe/name %)) (apply concat (vals recipes))))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
