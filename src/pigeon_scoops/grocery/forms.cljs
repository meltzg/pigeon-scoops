(ns pigeon-scoops.grocery.forms
  (:require
   ["@ant-design/icons" :refer [MinusCircleOutlined]]
   [antd :refer [Button Divider Flex Form Input InputNumber Spin]]
   [pigeon-scoops.components.form-actions :refer [form-actions]]
   [pigeon-scoops.components.constants-selector :refer [constants-selector]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! post-fetcher! put-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-groceries use-grocery
                                use-token]]
   [pigeon-scoops.utils.entity :refer [determine-ops]]
   [pigeon-scoops.utils.transform :refer [parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defn grocery-data->form-values [grocery]
  (-> grocery
      (update :grocery/department stringify-keyword)
      (update :grocery/units
              (fn [units]
                (mapv #(-> %
                           (update :grocery-unit/unit-mass-type stringify-keyword)
                           (update :grocery-unit/unit-volume-type stringify-keyword)
                           (update :grocery-unit/unit-common-type stringify-keyword))
                      units)))))

(defn unit-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :grocery-unit/unit-mass-type parse-keyword)
      (update :grocery-unit/unit-volume-type parse-keyword)
      (update :grocery-unit/unit-common-type parse-keyword)))

(defn grocery-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :grocery/department parse-keyword)
      (update :grocery/units (fn [units]
                               (map unit-form-values->data
                                    units)))))

(defn unit->comparable
  ([unit]
   (unit->comparable unit []))
  ([unit additional-keys]
   (->> (-> unit
            (unit-form-values->data)
            (select-keys (concat [:grocery-unit/source
                                  :grocery-unit/unit-cost
                                  :grocery-unit/unit-mass
                                  :grocery-unit/unit-mass-type
                                  :grocery-unit/unit-volume
                                  :grocery-unit/unit-volume-type
                                  :grocery-unit/unit-common
                                  :grocery-unit/unit-common-type]
                                 additional-keys)))
        (remove (comp nil? second))
        (into {}))))

(defn grocery->comparable [grocery]
  (->> (-> grocery
           (grocery-form-values->data)
           (select-keys [:grocery/name
                         :grocery/department
                         :grocery/units])
           (update :grocery/units #(map unit->comparable %)))
       (remove (comp nil? second))
       (into {})))

(defn on-finish [initial-grocery token values]
  (let [grocery (grocery-form-values->data values)
        grocery-id (atom (:grocery/id grocery))
        grocery-unit-ops (-> (determine-ops :grocery-unit/id
                                            (:grocery/units initial-grocery)
                                            (:grocery/units grocery)
                                            #(unit->comparable % [:grocery-unit/id]))
                             (update-vals (fn [vals]
                                            (map #(if (map? %)
                                                    (->> %
                                                         (remove (comp nil? second))
                                                         (into {}))
                                                    %)
                                                 vals))))
        headers {"Content-Type" "application/transit+json"}]
    (-> (if (nil? @grocery-id)
          (-> (post-fetcher!
               (str base-url "/groceries")
               {:token token
                :body (->> grocery
                           (remove #(nil? (second %)))
                           (into {}))
                :headers headers})
              (.then #(do
                        (reset! grocery-id (:id %))
                        (rfe/push-state :pigeon-scoops.grocery.routes/grocery
                                        {:grocery-id @grocery-id}))))
          (put-fetcher! (str base-url "/groceries/" @grocery-id) {:token token :body grocery :headers headers}))
        (.then (fn [_]
                 (js/Promise.all (clj->js (concat
                                           (map #(post-fetcher! (str base-url "/groceries/" @grocery-id "/units")
                                                                {:token token :body % :headers headers})
                                                (:new grocery-unit-ops))
                                           (map #(put-fetcher! (str base-url "/groceries/" @grocery-id "/units")
                                                               {:token token :body % :headers headers}) (:update grocery-unit-ops))
                                           (map #(delete-fetcher! (str base-url "/groceries/" @grocery-id "/units")
                                                                  {:token token :body {:grocery-unit/id %} :headers headers}) (:delete grocery-unit-ops)))))))
        (.then #(invalidate-groceries))
        (.catch (fn [e]
                  (js/alert (str "Error saving grocery: " (.-message e))))))))

(defn on-delete [token grocery-id]
  (-> (delete-fetcher! (str base-url "/groceries/" grocery-id) {:token token})
      (.then (fn [_]
               (invalidate-groceries)
               (rfe/push-state :pigeon-scoops.grocery.routes/groceries)))
      (.catch (fn [error]
                (js/alert (str "Error deleting grocery: " (.-message error)))))))

(defui grocery-form [{:keys [grocery-id]}]
  (let [{:keys [token]} (use-token)
        {:keys [grocery loading?]} (use-grocery grocery-id)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        all-values (Form.useWatch nil form)]

    (uix/use-effect
     (fn []
       (when grocery
         (let [form-values (grocery-data->form-values grocery)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form grocery])

    (uix/use-effect
     (fn []
       (set-unsaved-changes! (apply not= (map grocery->comparable [grocery all-values]))))
     [grocery all-values])

    (if (or loading? (and (not= grocery-id :new) (not (uuid? grocery-id))))
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish grocery token all-values)
               :style {:width "100%"}
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ form-actions {:form form
                          :entity-id grocery-id
                          :unsaved-changes? unsaved-changes?
                          :on-return #(rfe/push-state :pigeon-scoops.grocery.routes/groceries)
                          :on-delete (partial on-delete token grocery-id)})
         ($ Form.Item {:hidden true :name (stringify-keyword :grocery/id)}
            ($ Input))
         ($ Form.Item {:name (stringify-keyword :grocery/name) :label "Name" :rules (clj->js [{:required true :message "Item name"}])}
            ($ Input))
         ($ constants-selector {:form-item-name (stringify-keyword :grocery/department)
                                :label "Department"
                                :constants-key :constants/departments
                                :required? true})
         ($ Form.List {:name (stringify-keyword :grocery/units)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)]
                     ($ Flex {:key key :direction "row" :wrap true}
                        ($ Form.Item {:hidden true :name (clj->js [field-name (stringify-keyword :grocery-unit/id)])}
                           ($ Input))
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :grocery-unit/source)])
                                      :label "Source"
                                      :rules (clj->js [{:required true}])}
                           ($ Input))
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :grocery-unit/unit-cost)])
                                      :label "Unit Cost"
                                      :rules (clj->js [{:required true}])}
                           ($ InputNumber))
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :grocery-unit/unit-common)])}
                           ($ InputNumber {:placeholder "Common units"}))
                        ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :grocery-unit/unit-common-type)])
                                               :constants-key :constants/unit-types
                                               :valid-namespaces [:common]})
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :grocery-unit/unit-mass)])}
                           ($ InputNumber {:placeholder "Mass units"}))
                        ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :grocery-unit/unit-mass-type)])
                                               :constants-key :constants/unit-types
                                               :valid-namespaces [:mass]})
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :grocery-unit/unit-volume)])}
                           ($ InputNumber {:placeholder "Volume units"}))
                        ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :grocery-unit/unit-volume-type)])
                                               :constants-key :constants/unit-types
                                               :valid-namespaces [:volume]})
                        ($ Form.Item
                           ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)}))
                        ($ Divider))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Unit")))))))))
