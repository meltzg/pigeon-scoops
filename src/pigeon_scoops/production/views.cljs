(ns pigeon-scoops.production.views
  (:require
   ["@ant-design/icons" :refer [CheckOutlined]]
   [antd :refer [Button Checkbox Flex Spin Table]]
   [pigeon-scoops.hooks :refer [use-menus use-production-items use-recipes
                                use-token]]
   [pigeon-scoops.production.forms :refer [complete-recipe! format-amount
                                           production-items->rows]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defn amount-column [title amount-key amount-unit-key]
  {:title title
   :dataIndex (stringify-keyword amount-key)
   :render (fn [_ record]
             (let [parsed (js->clj record :keywordize-keys true)]
               (format-amount (get parsed amount-key) (get parsed amount-unit-key))))
   :key (keyword (name amount-key))})

(defn recipe-column []
  (merge {:title "Recipe"
          :dataIndex (stringify-keyword :recipe/name)
          :render (fn [_ record]
                    (let [{:order-item/keys [recipe-id amount amount-unit]
                           :recipe/keys [name]} (js->clj record :keywordize-keys true)]
                      ($ :a {:href (rfe/href :pigeon-scoops.recipe.routes/recipe
                                             {:recipe-id recipe-id}
                                             {:amount amount :amount-unit amount-unit})}
                         name)))
          :sorter (make-sorter :recipe/name)
          :key :recipe}
         (make-filter :recipe/name)))

(defn actions-column [token]
  {:title "Actions"
   :render (fn [_ record]
             (let [recipe-id (:order-item/recipe-id (js->clj record :keywordize-keys true))]
               ($ Button {:type "text"
                          :icon ($ CheckOutlined)
                          :on-click #(complete-recipe! token recipe-id)}
                  "Mark Complete")))
   :key :actions})

(defn make-recipe-columns [token]
  [(recipe-column)
   (amount-column "Amount To Make" :order-item/amount :order-item/amount-unit)
   (actions-column token)])

(defn make-size-columns []
  [(recipe-column)
   (amount-column "Size" :menu-item-size/amount :menu-item-size/amount-unit)
   {:title "Quantity Needed"
    :dataIndex (stringify-keyword :order-item/menu-item-size-quantity)
    :sorter (make-sorter :order-item/menu-item-size-quantity)
    :key :quantity}])

(defn rows->data-source [rows]
  (clj->js (map-indexed (fn [idx row] (assoc row :key idx)) rows)
           :keyword-fn stringify-keyword))

(defui item-production-view []
  (let [{:keys [production-items loading?]} (use-production-items true)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        {:keys [menus] menus-loading? :loading?} (use-menus true true)]
    (if (or loading? recipes-loading? menus-loading?)
      ($ Spin)
      ($ Table {:columns (clj->js (make-size-columns))
                :dataSource (rows->data-source (production-items->rows recipes menus production-items))
                :bordered true}))))

(defui recipe-production-view []
  (let [{:keys [production-items loading?]} (use-production-items false)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        {:keys [token] token-loading? :loading?} (use-token)]
    (if (or loading? recipes-loading? token-loading?)
      ($ Spin)
      ($ Table {:columns (clj->js (make-recipe-columns token))
                :dataSource (rows->data-source (production-items->rows recipes [] production-items))
                :bordered true}))))

(defui production-view []
  (let [[separate-sizes? set-separate-sizes!] (uix/use-state false)]
    ($ Flex {:gap "small" :vertical true :style {:height "100%"}}
       ($ Checkbox {:checked separate-sizes?
                    :on-change #(set-separate-sizes! (.. % -target -checked))}
          "Separate Sizes")
       (if separate-sizes?
         ($ item-production-view)
         ($ recipe-production-view)))))
