(ns pigeon-scoops.user-order.forms
  (:require
   [antd :refer [Button Flex Form Input InputNumber Space Spin]]
   [pigeon-scoops.api :refer [base-url]]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredients-selector
                                                        parse-ingredient
                                                        ingredient->option]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! post-fetcher! put-fetcher!]]
   [pigeon-scoops.hooks :refer [invalidate-orders use-order use-token]]
   [pigeon-scoops.utils :refer [determine-ops parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defn order-data->form-values [order]
  (-> order
      (update :user-order/status stringify-keyword)
      (update :user-order/items
              (fn [items]
                (map #(-> %
                          (update :order-item/amount-unit stringify-keyword)
                          (update :order-item/status stringify-keyword)
                          (assoc :order-item/recipe-id (ingredient->option
                                                        {:recipe :order-item/recipe-id}
                                                        %)))
                     items)))))

(defn item-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :order-item/amount-unit parse-keyword)
      (update :order-item/status parse-keyword)
      (parse-ingredient :order-item/recipe-id {:recipe :order-item/recipe-id})))

(defn order-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :user-order/status parse-keyword)
      (update :user-order/items
              (fn [items]
                (map item-form-values->data
                     items)))))

(defn item->comparable
  ([item]
   (item->comparable item []))
  ([item additional-keys]
   (-> item
       item-form-values->data
       (select-keys (concat [:order-item/status
                             :order-item/recipe-id
                             :order-item/amount
                             :order-item/amount-unit]
                            additional-keys)))))

(defn order->comparable [order]
  (-> order
      order-form-values->data
      (select-keys [:user-order/note
                    :user-order/status
                    :user-order/items])
      (update :user-order/items #(map item->comparable %))))

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

    (if (or loading? (not order))
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish order token all-values)
               :style {:width "100%"}
               :initialValues (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ Space {:align "start"}
            ($ Button {:html-type "button"
                       :disabled unsaved-changes?
                       :on-click #(rfe/push-state :pigeon-scoops.user-order.routes/orders)} "Return to Orders")
            ($ Button {:type "primary" :html-type "submit" :disabled (not unsaved-changes?)}
               (if (uuid? order-id) "Update Order" "Create Order"))
            ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")
            ($ Button {:html-type "button" :danger true :on-click (partial on-delete token order-id)} "Delete"))
         ($ Form.Item {:label "Note"
                       :name (stringify-keyword :user-order/note)
                       :rules (clj->js [{:required false}])}
            ($ Input))
         ($ constants-selector {:form-item-name (stringify-keyword :user-order/status)
                                :label "Status"
                                :constants-key :constants/order-statuses
                                :required true})
         ($ Form.List {:name (stringify-keyword :user-order/items)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :kewordize-keys true)]
                     ($ Flex {:direction "row" :key key}
                        ($ Form.Item {:hidden true
                                      :name (clj->js [field-name (stringify-keyword :order-item/id)])}
                           ($ Input))
                        ($ ingredients-selector {:form-item-name (clj->js [field-name (stringify-keyword :order-item/recipe-id)])
                                                 :ingredient-keys {:recipe :order-item/recipe-id}})
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :order-item/amount)])
                                      :rules (clj->js [{:required true}])}
                           ($ InputNumber))
                        ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :order-item/amount-unit)])
                                               :constants-key :constants/unit-types
                                               :required-true true})))))))))))