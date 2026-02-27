(ns pigeon-scoops.recipe.forms
  (:require
   ["@ant-design/icons" :refer [MinusCircleOutlined]]
   [antd :refer [Button Flex Form Input InputNumber Space Spin Switch]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as str]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredient->option ingredients-selector]]
   [pigeon-scoops.hooks :refer [use-recipe]]
   [uix.core :as uix :refer [$ defui]]))

(def TextArea (.-TextArea Input))

(defn on-finish [values]
  (prn "Submit:")
  (pprint values))

(defn data->form-values [data]
  (-> data
      (update :recipe/instructions #(str/join "\n" %))
      (update :recipe/ingredients
              (fn [ingredients]
                (map #(assoc % :ingredient/ingredient-id (ingredient->option %))
                     ingredients)))))

(defui recipe-form [{:keys [recipe-id]}]
  (let [{:keys [recipe loading?]} (use-recipe recipe-id)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        mystery? (Form.useWatch ":recipe/is-mystery" form)]

    (uix/use-effect
     (fn []
       (pprint (clj->js (data->form-values recipe) :keyword-fn str))
       (when recipe
         (let [form-values (data->form-values recipe)]
           (.setFieldsValue form (clj->js form-values :keyword-fn str))
           (set-initial-values! form-values))))
     [form recipe])

    (if (or loading? (not recipe))
      ($ Spin)
      ($ Form {:form form :on-finish on-finish :style {:width "100%"} :initial-values (clj->js initial-values :keyword-fn str)}
         ($ Form.Item {:hidden true :name (str :recipe/id)}
            ($ Input))
         ($ Form.Item
            ($ Space
               ($ Button {:type "primary" :html-type "submit"}
                  (if recipe-id "Update Recipe" "Create Recipe"))
               ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")))
         ($ Form.Item {:label "Name" :name (str :recipe/name) :rules (clj->js [{:required true}])}
            ($ Input))
         ($ Form.Item {:label "Public" :name (str :recipe/public)}
            ($ Switch))
         ($ Form.Item {:label "Mystery Flavor" :name (str :recipe/is-mystery)}
            ($ Switch))
         ($ Form.Item {:label "Source" :name (str :recipe/source)}
            ($ Input))
         ($ Form.Item {:label "Description" :name (str :recipe/description)}
            ($ TextArea))
         ($ Form.Item {:hidden (not mystery?) :label "Mystery Description" :name (str :recipe/mystery-description)}
            ($ TextArea))
         ($ Flex {:direction "row"}
            ($ Form.Item {:label "Amount" :name (str :recipe/amount) :rules (clj->js [{:required true}])}
               ($ InputNumber))
            ($ constants-selector {:form-item-name (str :recipe/amount-unit) :constants-key :constants/unit-types :required? true}))
         ($ Form.Item {:label "Instructions" :name (str :recipe/instructions)}
            ($ TextArea))
         ($ Form.List {:name (str :recipe/ingredients)}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)]
                     ($ Flex {:direction "row" :key key}
                        ($ Form.Item {:hidden true :name (clj->js [field-name (str :ingredient/id)])}
                           ($ Input))
                        ($ ingredients-selector {:form-item-name (clj->js [field-name (str :ingredient/ingredient-id)])})
                        ($ Form.Item {:name (clj->js [field-name (str :ingredient/amount)])
                                      :rules (clj->js [{:required true}])}
                           ($ InputNumber))
                        ($ constants-selector {:form-item-name (clj->js [field-name (str :ingredient/amount-unit)])
                                               :constants-key :constants/unit-types
                                               :required? true})
                        ($ Form.Item
                           ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)})))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Ingredient")))))))))
