(ns pigeon-scoops.recipe.forms
  (:require
   ["@ant-design/icons" :refer [MinusCircleOutlined]]
   [antd :refer [Button Flex Form Input InputNumber Space Spin Switch]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as str]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredient->option ingredients-selector parse-ingredient]]
   [pigeon-scoops.hooks :refer [use-recipe]]
   [pigeon-scoops.utils :refer [parse-keyword stringify-keyword]]
   [uix.core :as uix :refer [$ defui]]))

(def TextArea (.-TextArea Input))

(defn data->form-values [data]
  (-> data
      (update :recipe/instructions #(str/join "\n" %))
      (update :recipe/amount-unit stringify-keyword)
      (update :recipe/ingredients
              (fn [ingredients]
                (map #(-> %
                          (update :ingredient/amount-unit stringify-keyword)
                          (assoc :ingredient/ingredient-id (ingredient->option %))
                          (update-keys stringify-keyword))
                     ingredients)))
      (update-keys stringify-keyword)))

(defn form-values->data [form-values]
  (-> form-values
      (js->clj :keywordize-keys true)
      (update :recipe/instructions #(str/split % #"\n"))
      (update :recipe/amount-unit parse-keyword)
      (update :recipe/ingredients
              (fn [ingredients]
                (map #(-> %
                          (update :ingredient/amount-unit parse-keyword)
                          (parse-ingredient))
                     ingredients)))))

(defn on-finish [values]
  (prn "Submit:")
  (pprint (form-values->data values)))

(defui recipe-form [{:keys [recipe-id]}]
  (let [{:keys [recipe loading?]} (use-recipe recipe-id)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [initial-form-data set-initial-form-data!] (uix/use-state nil)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        mystery? (Form.useWatch "recipe/is-mystery" form)
        all-values (Form.useWatch (clj->js []) form)]

    (uix/use-effect
     (fn []
       (when recipe
         (let [form-values (data->form-values recipe)]
           (.setFieldsValue form (clj->js form-values :keyword-fn str))
           (set-initial-values! form-values))))
     [form recipe])
    
    (uix/use-effect
     (fn []
       (when (and (not (nil? initial-values)) (nil? initial-form-data))
         (set-initial-form-data! (form-values->data (.getFieldsValue form))))
       (set-unsaved-changes! (not= initial-form-data (form-values->data all-values))))
     [form all-values initial-values initial-form-data])

    (if (or loading? (not recipe))
      ($ Spin)
      ($ Form {:form form :on-finish on-finish :style {:width "100%"} :initial-values (clj->js initial-values :keyword-fn str)}
         ($ Form.Item
            ($ Space
               ($ Button {:type "primary" :html-type "submit" :disabled (not unsaved-changes?)}
                  (if recipe-id "Update Recipe" "Create Recipe"))
               ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")))
         ($ Form.Item {:hidden true :name (stringify-keyword :recipe/id)}
            ($ Input))
         ($ Form.Item {:label "Name" :name (stringify-keyword :recipe/name) :rules (clj->js [{:required true}])}
            ($ Input))
         ($ Form.Item {:label "Public" :name (stringify-keyword :recipe/public)}
            ($ Switch))
         ($ Form.Item {:label "Mystery Flavor" :name (stringify-keyword :recipe/is-mystery)}
            ($ Switch))
         ($ Form.Item {:label "Source" :name (stringify-keyword :recipe/source)}
            ($ Input))
         ($ Form.Item {:label "Description" :name (stringify-keyword :recipe/description)}
            ($ TextArea))
         ($ Form.Item {:hidden (not mystery?) :label "Mystery Description" :name (stringify-keyword :recipe/mystery-description)}
            ($ TextArea))
         ($ Flex {:direction "row"}
            ($ Form.Item {:label "Amount" :name (stringify-keyword :recipe/amount) :rules (clj->js [{:required true}])}
               ($ InputNumber))
            ($ constants-selector {:form-item-name (stringify-keyword :recipe/amount-unit) :constants-key :constants/unit-types :required? true}))
         ($ Form.Item {:label "Instructions" :name (stringify-keyword :recipe/instructions)}
            ($ TextArea))
         ($ Form.List {:name (stringify-keyword :recipe/ingredients)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)]
                     ($ Flex {:direction "row" :key key}
                        ($ Form.Item {:hidden true :name (clj->js [field-name (stringify-keyword :ingredient/id)])}
                           ($ Input))
                        ($ ingredients-selector {:form-item-name (clj->js [field-name (stringify-keyword :ingredient/ingredient-id)])})
                        ($ Form.Item {:name (clj->js [field-name (stringify-keyword :ingredient/amount)])
                                      :rules (clj->js [{:required true}])}
                           ($ InputNumber))
                        ($ constants-selector {:form-item-name (clj->js [field-name (stringify-keyword :ingredient/amount-unit)])
                                               :constants-key :constants/unit-types
                                               :required? true})
                        ($ Form.Item
                           ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)})))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Ingredient")))))))))
