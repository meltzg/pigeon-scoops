(ns pigeon-scoops.user-order.forms
  (:require
   ["@ant-design/icons" :refer [ExportOutlined MinusCircleOutlined]]
   [antd :refer [Button Flex Form Input InputNumber Space Spin]]
   [pigeon-scoops.components.form-actions :refer [form-actions]]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredient->option
                                                        ingredients-selector
                                                        parse-ingredient]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! post-fetcher! put-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-orders use-order use-token]]
   [pigeon-scoops.utils :refer [determine-ops parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defn order-data->form-values [order]
  (-> order
      (update :user-order/status stringify-keyword)
      (update :user-order/items
              (fn [items]
                (mapv #(-> %
                           (update :order-item/amount-unit stringify-keyword)
                           (update :order-item/status stringify-keyword)
                           (assoc :order-item/ingredient-id (ingredient->option
                                                             {:recipe :order-item/recipe-id}
                                                             %)))
                      items)))))

(defn item-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :order-item/amount-unit parse-keyword)
      (update :order-item/status parse-keyword)
      ((partial parse-ingredient
                :order-item/ingredient-id
                {:recipe :order-item/recipe-id}))))

(defn order-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :user-order/status parse-keyword)
      (update :user-order/items (fn [items]
                                  (map item-form-values->data
                                       items)))))

(defn item->comparable
  ([item]
   (item->comparable item []))
  ([item additional-keys]
   (->> (-> item
            (item-form-values->data)
            (select-keys (concat [:order-item/amount
                                  :order-item/amount-unit
                                  :order-item/status
                                  :order-item/recipe-id]
                                 additional-keys)))
        (remove (comp nil? second))
        (into {}))))

(defn order->comparable [order]
  (->> (-> order
           (order-form-values->data)
           (select-keys [:user-order/note
                         :user-order/status
                         :user-order/items])
           (update :user-order/items #(map item->comparable %)))
       (remove (comp nil? second))
       (into {})))

(defn on-finish [initial-order token values]
  (let [order (order-form-values->data values)
        order-id (atom (:user-order/id order))
        order-item-ops (-> (determine-ops :order-item/id
                                          (:user-order/items initial-order)
                                          (:user-order/items order)
                                          #(item->comparable % [:order-item/id]))
                           (update-vals (fn [vals]
                                          (map #(if (map? %)
                                                  (->> %
                                                       (remove (comp nil? second))
                                                       (into {}))
                                                  %)
                                               vals))))
        headers {"Content-Type" "application/transit+json"}]
    (-> (if (nil? @order-id)
          (-> (post-fetcher!
               (str base-url "/orders")
               {:token token
                :body (->> order
                           (remove #(nil? (second %)))
                           (into {}))
                :headers headers})
              (.then #(do
                        (reset! order-id (:id %))
                        (rfe/push-state :pigeon-scoops.user-order.routes/order
                                        {:order-id @order-id}))))
          (put-fetcher! (str base-url "/orders/" @order-id) {:token token :body order :headers headers}))
        (.then (fn [_]
                 (js/Promise.all (clj->js (concat
                                           (map #(post-fetcher! (str base-url "/orders/" @order-id "/items")
                                                                {:token token :body % :headers headers})
                                                (:new order-item-ops))
                                           (map #(put-fetcher! (str base-url "/orders/" @order-id "/items")
                                                               {:token token :body % :headers headers}) (:update order-item-ops))
                                           (map #(delete-fetcher! (str base-url "/orders/" @order-id "/items")
                                                                  {:token token :body {:order-item/id %} :headers headers}) (:delete order-item-ops)))))))
        (.then #(invalidate-orders))
        (.catch (fn [e]
                  (js/alert (str "Error saving order: " (.-message e))))))))

(defn on-delete [token order-id]
  (-> (delete-fetcher! (str base-url "/orders/" order-id) {:token token})
      (.then (fn [_]
               (invalidate-orders)
               (rfe/push-state :pigeon-scoops.user-order.routes/orders)))
      (.catch (fn [error]
                (js/alert (str "Error deleting order: " (.-message error)))))))

(defui order-form [{:keys [order-id]}]
  (let [{:keys [token]} (use-token)
        {:keys [order loading?]} (use-order order-id)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        all-values (Form.useWatch nil form)]

    (uix/use-effect
     (fn []
       (when order
         (let [form-values (order-data->form-values order)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form order])

    (uix/use-effect
     (fn []
       (set-unsaved-changes! (apply not= (map order->comparable [order all-values]))))
     [order all-values])

    (if (or loading? (and (not= order-id :new) (not (uuid? order-id))))
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish order token all-values)
               :style {:width "100%"}
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ form-actions {:form form
                          :entity-id order-id
                          :unsaved-changes? unsaved-changes?
                          :on-return #(rfe/push-state :pigeon-scoops.user-order.routes/orders)
                          :on-delete (partial on-delete token order-id)})
         ($ Form.Item {:hidden true :name (stringify-keyword :user-order/id)}
            ($ Input))
         ($ Form.Item {:name (stringify-keyword :user-order/note) :label "Note" :rules (clj->js [{:required true}])}
            ($ Input))
         ($ constants-selector {:form-item-name (stringify-keyword :user-order/status)
                                :label "Status"
                                :constants-key :constants/order-statuses
                                :required? true})
         ($ Form.List {:name (stringify-keyword :user-order/items)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)
                         item (get (js->clj (.getFieldValue form (clj->js [[(stringify-keyword :user-order/items)]]))
                                            :keywordize-keys true)
                                   field-name)
                         parsed-item (when (:order-item/ingredient-id item)
                                       (item-form-values->data item))]
                     ($ Flex {:key key :direction "row"}
                        ($ Form.Item {:hidden true :name (clj->js [field-name (stringify-keyword :order-item/id)])}
                           ($ Input))
                        ($ Flex {:vertical true}
                           ($ Space {:align "start"}
                              ($ ingredients-selector {:form-item-name (clj->js [field-name (stringify-keyword :order-item/ingredient-id)])
                                                       :ingredient-keys {:recipe :order-item/recipe-id}
                                                       :required? true})
                              ($ Form.Item {:name (clj->js [field-name (stringify-keyword :order-item/amount)])
                                            :rules (clj->js [{:required true}])}
                                 ($ InputNumber {:placeholder "Amount"}))
                              ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :order-item/amount-unit)])
                                                     :constants-key :constants/unit-types
                                                     :required? true})
                              ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :order-item/status)])
                                                     :label "Status"
                                                     :constants-key :constants/order-statuses
                                                     :required? true})
                              ($ Form.Item
                                 ($ Button {:type "text"
                                            :disabled (or (nil? (:order-item/id parsed-item))
                                                          unsaved-changes?)
                                            :icon ($ ExportOutlined)
                                            :on-click #(rfe/push-state
                                                        :pigeon-scoops.recipe.routes/recipe
                                                        {:recipe-id (:order-item/recipe-id parsed-item)}
                                                        {:amount (:order-item/amount parsed-item)
                                                         :amount-unit (:order-item/amount-unit parsed-item)})})
                                 ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)})))))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add item")))))))))
