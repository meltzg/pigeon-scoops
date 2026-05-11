(ns pigeon-scoops.storefront.forms
  (:require
   [antd :refer [Card
                 Divider
                 Flex
                 Form
                 Input
                 Spin
                 Typography]]
   [pigeon-scoops.hooks :refer [use-recipes
                                use-active-order
                                use-menus
                                use-token]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [uix.core :refer [$ defui] :as uix]))

(defn menu-order-data->storefront-form-values [menus active-order]
  (let [menu-items (mapcat :menu/items menus)
        size->order-item (->> active-order
                              :user-order/items
                              (map #(vector (:order-item/menu-item-size-id %)))
                              (into {}))]
    {:storefront/items (map
                        (fn [menu-item]
                          (update menu-item :menu-item/sizes
                                  #(map (fn [size]
                                          (-> size
                                              (assoc :menu-item-size/order-quantity
                                                 (get-in size->order-item
                                                         [(:menu-item-size/id size)
                                                          :order-item/amount]
                                                         0))
                                              (update :menu-item-size/amount-unit stringify-keyword)))
                                        %)))
                        menu-items)}))

(defn item-size-form-value->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :menu-item-size/amount-unit parse-keyword)))

(defn item-form-value->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :storefront/items #(map (fn [item]
                                        (update item :menu-item/sizes
                                                (fn [sizes]
                                                  (map item-size-form-value->data sizes))))
                                      %))))

(defui storefront-form []
  (let [{:keys [token]} (use-token)
        {:keys [menus] menues-loading? :loading?} (use-menus false true)
        {:keys [active-order] order-loading? :loading?} (use-active-order)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)]

    (uix/use-effect
     (fn []
       (when (and menus (not order-loading?))
         (let [form-values (menu-order-data->storefront-form-values menus active-order)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form menus order-loading? active-order])

    (if (or menues-loading? order-loading?)
      ($ Spin)
      ($ Form {:form form
               :style {:width "100%"}
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ Form.List {:name (stringify-keyword :storefront/items)}
            (fn [item-fields _]
              ($ Card {:title "Current Flavors"}
                 (for [item-field item-fields]
                   (let [{:keys [key] item-name :name} (js->clj item-field :keywordize-keys true)
                         parsed-item (->> item-name
                                          (get (js->clj (.getFieldValue form (clj->js [[(stringify-keyword :storefront/items)]]))
                                                        :keywordize-keys true))
                                          (item-form-value->data))
                         recipe (first (filter #(= (:recipe/id %)
                                                   (:menu-item/recipe-id parsed-item))
                                               recipes))]
                     ($ Card {:key key :title (:recipe/name recipe)} 
                        ($ Card.Meta {:description (:recipe/description recipe)})
                        ($ Divider)
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/menu-id)])}
                           ($ Input))
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/id)])}
                           ($ Input))
                        ($ Form.List {:name (clj->js [item-name (stringify-keyword :menu-item/sizes)])}
                           (fn [size-fields _]
                             ($ :div
                                (for [size-field size-fields]
                                  (let [{:keys [key] size-name :name} (js->clj size-field :keywordize-keys true)
                                        parsed-size (->> item-name
                                                         (get (js->clj (.getFieldValue form (clj->js [[(stringify-keyword :storefront/items)]]))
                                                                       :keywordize-keys true))
                                                         :menu-item/sizes
                                                         (#(get % size-name)))]
                                    ($ Flex {:key key :wrap true}
                                       ($ Typography (str (:menu-item-size/amount parsed-size)
                                                          (-> parsed-size
                                                              :menu-item-size/amount-unit
                                                              (parse-keyword)
                                                              (name))))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/id)])}
                                          ($ Input))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/amount)])}
                                          ($ Input))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/amount-unit)])}
                                          ($ Input))
                                       ($ Divider)))))))))))))))))
