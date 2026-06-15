(ns pigeon-scoops.storefront.forms
  (:require
   ["@ant-design/icons" :refer [ExportOutlined]]
   [antd :refer [Button Card Divider Flex Form Input InputNumber Spin
                 Typography]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! patch-fetcher!
                                   post-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-menus! invalidate-orders!
                                use-active-order use-menus use-recipes
                                use-token]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :refer [$ defui] :as uix]))

(defn menu-order-data->storefront-form-values [menus active-order]
  (let [menus (->> menus
                   (map #(vector (:menu/id %) %))
                   (into {}))
        size->order-item (->> active-order
                              :user-order/items
                              (map #(vector (:order-item/menu-item-size-id %) %))
                              (into {}))]
    {:storefront/items (->> menus
                            (vals)
                            (mapcat :menu/items)
                            (map
                             (fn [menu-item]
                               (-> menu-item
                                   (assoc :menu-item/end-time (get-in menus [(:menu-item/menu-id menu-item) :menu/end-time]))
                                   (update  :menu-item/sizes
                                            #(map (fn [size]
                                                    (-> size
                                                        (assoc :menu-item-size/order-quantity
                                                               (/ (get-in size->order-item
                                                                          [(:menu-item-size/id size)
                                                                           :order-item/amount]
                                                                          0)
                                                                  (:menu-item-size/amount size)))
                                                        (update :menu-item-size/amount-unit stringify-keyword)))
                                                  %))))))}))

(defn item-size-form-value->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :menu-item-size/amount-unit parse-keyword)))

(defn storefront-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :storefront/items #(map (fn [item]
                                        (update item :menu-item/sizes
                                                (fn [sizes]
                                                  (map item-size-form-value->data sizes))))
                                      %))))

(defn storefront-save-ops [order values]
  (let [size->order-item (->> order
                              :user-order/items
                              (map #(vector (:order-item/menu-item-size-id %) %))
                              (into {}))
        storefront-sizes (->> values
                              (storefront-form-values->data)
                              :storefront/items
                              (mapcat (fn [item]
                                        (map #(assoc % :menu-item-size/menu-item-id (:menu-item/id item)
                                                     :menu-item-size/menu-id (:menu-item/menu-id item)
                                                     :menu-item-size/recipe-id (:menu-item/recipe-id item))
                                             (:menu-item/sizes item)))))]
    {:size->order-item size->order-item
     :storefront-sizes storefront-sizes}))

(defn on-finish! [order token user values]
  (let [{:keys [size->order-item storefront-sizes]} (storefront-save-ops order values)
        headers {"Content-Type" "application/transit+json"}]
    (-> (js/Promise.resolve (if order
                              (:user-order/id order)
                              (-> (post-fetcher!
                                   (str base-url "/orders")
                                   {:token token
                                    :headers headers
                                    :body {:user-order/note (str (:name user) " " (->> (js/Date.now)
                                                                                       (js/Date.)
                                                                                       (.toISOString)))}})
                                  (.then #(:id %)))))
        (.then (fn [order-id]
                 (js/Promise.all
                  (map (fn [menu-size]
                         (let [existing-order-item (get size->order-item (:menu-item-size/id menu-size))
                               body {:order-item/menu-item-size-id (:menu-item-size/id menu-size)
                                     :order-item/recipe-id (:menu-item-size/recipe-id menu-size)
                                     :order-item/amount (* (:menu-item-size/order-quantity menu-size)
                                                           (:menu-item-size/amount menu-size))
                                     :order-item/amount-unit (:menu-item-size/amount-unit menu-size)}]
                           (cond (and existing-order-item (zero? (:menu-item-size/order-quantity menu-size)))
                                 (delete-fetcher! (str base-url "/orders/" order-id "/items/" (:order-item/id existing-order-item))
                                                  {:token token})
                                 existing-order-item
                                 (-> (patch-fetcher! (str base-url "/orders/" order-id "/items/" (:order-item/id existing-order-item))
                                                     {:token token
                                                      :headers headers
                                                      :body body})
                                     (.then (fn []
                                              (patch-fetcher! (str base-url "/orders/" order-id "/items/" (:order-item/id existing-order-item) "/status")
                                                              {:token token
                                                               :headers headers
                                                               :body {:order-item/status :status/submitted}}))))
                                 (and (not existing-order-item)
                                      (pos? (:menu-item-size/order-quantity menu-size)))
                                 (-> (post-fetcher! (str base-url "/orders/" order-id "/items")
                                                    {:token token
                                                     :headers headers
                                                     :body body})
                                     (.then (fn [{:keys [id]}]
                                              (patch-fetcher! (str base-url "/orders/" order-id "/items/" id "/status")
                                                              {:token token
                                                               :headers headers
                                                               :body {:order-item/status :status/submitted}})))))))
                       storefront-sizes))))
        (.then (fn []
                 (when (and order (every? #(zero? (:menu-item-size/order-quantity %)) storefront-sizes))
                   (delete-fetcher! (str base-url "/orders/" (:user-order/id order))
                                    {:token token}))))
        (.catch (fn [e]
                  (js/alert (or (-> e ex-data :body :message)
                                (.-message e)
                                "Error submitting order"))))
        (.finally (comp invalidate-orders! invalidate-menus!)))))

(defn on-reopen! [order token]
  (-> (js/Promise.all
       (map (fn [item]
              (patch-fetcher! (str base-url "/orders/" (:user-order/id order) "/items/" (:order-item/id item) "/status")
                              {:token token
                               :headers {"Content-Type" "application/transit+json"}
                               :body {:order-item/status :status/draft}}))
            (:user-order/items order)))
      (.finally (comp invalidate-orders! invalidate-menus!))))

(defui storefront-form []
  (let [{:keys [token user]} (use-token)
        {:keys [menus] menues-loading? :loading?} (use-menus false true)
        {:keys [active-order] order-loading? :loading?} (use-active-order)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)]

    (uix/use-effect
     (fn []
       (when (and menus (not order-loading?) (not recipes-loading?))
         (let [form-values (menu-order-data->storefront-form-values menus active-order)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form menus order-loading? active-order recipes-loading?])

    (if (or menues-loading? order-loading?)
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish! active-order token user)
               :disabled (= (:user-order/status active-order) :status/submitted)
               :style {:width "100%"}
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ Form.List {:name (stringify-keyword :storefront/items)}
            (fn [item-fields _]
              ($ Card {:title "Current Flavors"}
                 ($ Flex {:gap "small"}
                    (if (= (:user-order/status active-order) :status/submitted)
                      ($ Button {:disabled false :type "primary" :on-click #(on-reopen! active-order token)} "Edit Order")
                      ($ Button {:disabled false :html-type "submit" :type "primary"} "Submit Order"))
                    (when active-order
                      ($ Button {:disabled false
                                 :icon ($ ExportOutlined)
                                 :on-click #(rfe/push-state
                                             :pigeon-scoops.user-order.routes/order
                                             {:order-id (:user-order/id active-order)})}
                         "View Order")))
                 ($ Divider)
                 (for [item-field item-fields]
                   (let [{:keys [key] item-name :name} (js->clj item-field :keywordize-keys true)
                         parsed-item (->> item-name
                                          (get (js->clj (.getFieldValue form (clj->js [[(stringify-keyword :storefront/items)]]))
                                                        :keywordize-keys true))
                                          (storefront-form-values->data))
                         menu (->> menus
                                   (filter #(= (:menu/id %) (:menu-item/menu-id parsed-item)))
                                   (first))
                         recipe (first (filter #(= (:recipe/id %)
                                                   (:menu-item/recipe-id parsed-item))
                                               recipes))]
                     ($ Card {:key key :title ($ :a {:href (rfe/href :pigeon-scoops.recipe.routes/recipe {:recipe-id (:recipe/id recipe)})}
                                                 (:recipe/name recipe))}
                        ($ Card.Meta {:description ($ :div
                                                      (str "Accepting orders until " (:menu-item/end-time parsed-item))
                                                      ($ :br)
                                                      (if (:menu/repeats menu)
                                                        "This item will reopen for new orders after the current expiration."
                                                        "Limited time only!")
                                                      ($ :br)
                                                      (:recipe/description recipe))})
                        ($ Divider)
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/menu-id)])}
                           ($ Input))
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/end-time)])}
                           ($ Input))
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/id)])}
                           ($ Input))
                        ($ Form.Item {:hidden true :name (clj->js [item-name (stringify-keyword :menu-item/recipe-id)])}
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
                                    ($ Flex {:key key :wrap true :gap "small"}
                                       ($ Typography (str (:menu-item-size/amount parsed-size)
                                                          (-> parsed-size
                                                              :menu-item-size/amount-unit
                                                              (parse-keyword)
                                                              (name))))
                                       ($ Typography (let [aq (:menu-item-size/available-quantity parsed-size)]
                                                       (if (and (some? aq) (>= aq 0))
                                                         (str aq " available")
                                                         "Unlimited")))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/id)])}
                                          ($ Input))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/amount)])}
                                          ($ Input))
                                       ($ Form.Item {:hidden true :name (clj->js [size-name (stringify-keyword :menu-item-size/amount-unit)])}
                                          ($ Input))
                                       ($ Form.Item {:name (clj->js [size-name (stringify-keyword :menu-item-size/order-quantity)])}
                                          ($ InputNumber {:min 0}))
                                       ($ Divider)))))))))))))))))
