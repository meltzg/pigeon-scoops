(ns pigeon-scoops.menu.forms
  (:require
   ["@ant-design/icons" :refer [MinusCircleOutlined]]
   [antd :refer [Button Card Flex Form Input InputNumber Spin Switch]]
   [cljs.pprint :refer [pprint]]
   [pigeon-scoops.components.constants-selector :refer [constants-selector]]
   [pigeon-scoops.components.form-actions :refer [form-actions]]
   [pigeon-scoops.components.ingredients-selector :refer [ingredient->option
                                                          ingredients-selector
                                                          parse-ingredient]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! post-fetcher! put-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-menus use-menu use-token]]
   [pigeon-scoops.utils.entity :refer [determine-ops]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defn menu-data->form-values [menu]
  (-> menu
      (update :menu/duration-type stringify-keyword)
      (update :menu/items
              (fn [items]
                (mapv #(-> %
                           (assoc :menu-item/ingredient-id (ingredient->option
                                                            {:recipe :menu-item/recipe-id}
                                                            %)))
                      items)))))

(defn item-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      ((partial parse-ingredient
                :menu-item/ingredient-id
                {:recipe :menu-item/recipe-id}))))

(defn menu-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :menu/duration-type parse-keyword)
      (update :menu/items (fn [items]
                            (map item-form-values->data
                                 items)))))

(defn item->comparable
  ([item]
   (item->comparable item []))
  ([item additional-keys]
   (->> (-> item
            (item-form-values->data)
            (select-keys (concat [:menu-item/recipe-id]
                                 additional-keys)))
        (remove (comp nil? second))
        (into {}))))

(defn menu->comparable [menu]
  (->> (-> menu
           (menu-form-values->data)
           (select-keys [:menu/name
                         :menu/active
                         :menu/repeats
                         :menu/duration
                         :menu/duration-type 
                         :menu/items])
           (update :menu/items #(map item->comparable %)))
       (remove (comp nil? second))
       (into {})))

(defn on-finish [initial-menu token values]
  (let [menu (menu-form-values->data values)
        menu-id (atom (:menu/id menu))
        menu-item-ops (-> (determine-ops :menu-item/id
                                         (:menu/items initial-menu)
                                         (:menu/items menu)
                                         #(item->comparable % [:menu-item/id]))
                          (update-vals (fn [vals]
                                         (map #(if (map? %)
                                                 (->> %
                                                      (remove (comp nil? second))
                                                      (into {}))
                                                 %)
                                              vals))))
        headers {"Content-Type" "application/transit+json"}]
    (-> (if (nil? @menu-id)
          (-> (post-fetcher!
               (str base-url "/menus")
               {:token token
                :body (->> menu
                           (remove #(nil? (second %)))
                           (into {}))
                :headers headers})
              (.then #(do
                        (reset! menu-id (:id %))
                        (rfe/push-state :pigeon-scoops.menu.routes/menu
                                        {:menu-id @menu-id}))))
          (put-fetcher! (str base-url "/menus/" @menu-id) {:token token :body menu :headers headers}))
        (.then (fn [_]
                 (js/Promise.all (clj->js (concat
                                           (map #(post-fetcher! (str base-url "/menus/" @menu-id "/items")
                                                                {:token token :body % :headers headers})
                                                (:new menu-item-ops))
                                           (map #(put-fetcher! (str base-url "/menus/" @menu-id "/items")
                                                               {:token token :body % :headers headers}) (:update menu-item-ops))
                                           (map #(delete-fetcher! (str base-url "/menus/" @menu-id "/items")
                                                                  {:token token :body {:menu-item/id %} :headers headers}) (:delete menu-item-ops)))))))
        (.then #(invalidate-menus))
        (.catch (fn [e]
                  (js/alert (str "Error saving menu: " (.-message e))))))))

(defn on-delete [token menu-id]
  (-> (delete-fetcher! (str base-url "/menus/" menu-id) {:token token})
      (.then (fn [_]
               (invalidate-menus)
               (rfe/push-state :pigeon-scoops.menu.routes/menus)))
      (.catch (fn [error]
                (js/alert (str "Error deleting menu: " (.-message error)))))))

(defui menu-form [{:keys [menu-id]}]
  (let [{:keys [token]} (use-token)
        {:keys [menu loading?]} (use-menu menu-id)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        all-values (Form.useWatch nil form)]

    (uix/use-effect
     (fn []
       (when menu
         (let [form-values (menu-data->form-values menu)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form menu])

    (uix/use-effect
     (fn []
       (pprint (map menu->comparable [menu all-values]))
       (set-unsaved-changes! (apply not= (map menu->comparable [menu all-values]))))
     [menu all-values])

    (if (or loading? (and (not= menu-id :new) (not (uuid? menu-id))))
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish menu token all-values)
               :style {:width "100%"}
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ form-actions {:form form
                          :entity-id menu-id
                          :unsaved-changes? unsaved-changes?
                          :on-delete (partial on-delete token menu-id)
                          :on-return #(rfe/push-state :pigeon-scoops.menu.routes/menus)}) 
         ($ Form.Item {:hidden true :name (stringify-keyword :menu/id)}
            ($ Input))
         ($ Form.Item {:name (stringify-keyword :menu/name) :label "Name" :rules (clj->js [{:required true}])}
            ($ Input))
         ($ Form.Item {:name (stringify-keyword :menu/active) :label "Active" :valuePropName "checked"}
            ($ Switch))
         ($ Flex {:wrap true}
            ($ Form.Item {:name (stringify-keyword :menu/repeats) :label "Repeats" :valuePropName "checked"}
               ($ Switch))
            ($ Form.Item {:name (stringify-keyword :menu/duration) :label "Duration"}
               ($ InputNumber {:placeholder "Duration"}))
            ($ constants-selector {:form-item-name (stringify-keyword :menu/duration-type)
                                   :constants-key :constants/menu-durations}))
         ($ Form.List {:name (stringify-keyword :menu/items)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)]
                     ($ Card {:key key}
                        ($ Flex {:direction "row" :wrap true}
                        ($ Form.Item {:hidden true :name (clj->js [field-name (stringify-keyword :menu-item/id)])}
                           ($ Input))
                        ($ Flex {:vertical true}
                           ($ Flex {:wrap true :align "start"}
                              ($ ingredients-selector {:form-item-name (clj->js [field-name (stringify-keyword :menu-item/ingredient-id)])
                                                       :ingredient-keys {:recipe :menu-item/recipe-id}
                                                       :required? true}) 
                              ($ Form.Item 
                                 ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)}))))))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add item")))))))))
