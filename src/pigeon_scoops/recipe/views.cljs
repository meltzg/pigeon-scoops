(ns pigeon-scoops.recipe.views
  (:require
   ["@ant-design/icons" :refer [ExportOutlined FileAddOutlined]]
   [antd :refer [Button Space Spin Table]]
   [clojure.string :as str]
   [pigeon-scoops.hooks :refer [use-recipes]]
   [pigeon-scoops.recipe.forms :refer [recipe-form]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui recipe-view [{:keys [path query]}]
  (let [{:keys [recipe-id]} path
        {:keys [amount amount-unit original-recipe]} query]
    ($ recipe-form {:key (str recipe-id "|" amount "|" amount-unit "|" original-recipe)
                    :recipe-id recipe-id
                    :scaled-amount amount
                    :scaled-amount-unit amount-unit
                    :original-recipe original-recipe})))

(defn make-columns []
  [(merge {:title "Name"
           :dataIndex (stringify-keyword :recipe/name)
           :sorter (make-sorter :recipe/name)
           :key :name}
          (make-filter :recipe/name))
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
      ($ Table {:columns (clj->js (make-columns))
                :dataSource (clj->js (map-indexed (fn [idx recipe] (assoc recipe :key idx))
                                                  (sort-by #(str/lower-case (:recipe/name %)) recipes))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
