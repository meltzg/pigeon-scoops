(ns pigeon-scoops.recipe.views
  (:require
   ["@ant-design/icons" :refer [FileAddOutlined]]
   ["react-icons/md" :refer [MdDeleteForever]]
   [antd :refer [Button Space Spin Table]]
   [clojure.string :as str]
   [pigeon-scoops.fetchers :refer [delete-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-recipes! use-recipes
                                use-token]]
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

(defn make-columns [token]
  [(merge {:title "Name"
           :dataIndex (stringify-keyword :recipe/name)
           :render (fn [_ record]
                     (let [record (js->clj record :keywordize-keys true)]
                       ($ :a {:href (rfe/href :pigeon-scoops.recipe.routes/recipe {:recipe-id (:recipe/id record)})}
                          (:recipe/name record))))
           :sorter (make-sorter :recipe/name)
           :key :name}
          (make-filter :recipe/name))
   {:title ($ Space
              "Actions"
              ($ Button {:type "text"
                         :icon ($ FileAddOutlined)
                         :on-click #(rfe/push-state
                                     :pigeon-scoops.recipe.routes/recipe
                                     {:recipe-id :new})}
                 "New Recipe"))
    :render (fn [_ recipe]
              ($ Button {:type "text"
                         :icon ($ MdDeleteForever {:size 25})
                         :danger true
                         :on-click #(-> recipe
                                        (js->clj :keywordize-keys true)
                                        :recipe/id
                                        ((fn [recipe-id]
                                           (-> (delete-fetcher! (str base-url "/recipes/" recipe-id)
                                                                {:token token})
                                               (.then (fn []
                                                        (invalidate-recipes!)))))))}))}])

(defui recipes-table []
  (let [{:keys [recipes loading?]} (use-recipes)
        {:keys [token] token-loading? :loading?} (use-token)]
    (if (or loading? token-loading?)
      ($ Spin)
      ($ Table {:columns (clj->js (make-columns token))
                :dataSource (clj->js (map-indexed (fn [idx recipe] (assoc recipe :key idx))
                                                  (sort-by #(str/lower-case (:recipe/name %)) recipes))
                                     :keyword-fn stringify-keyword)
                :bordered true}))))
