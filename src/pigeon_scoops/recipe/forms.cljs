(ns pigeon-scoops.recipe.forms
  (:require  [uix.core :as uix :refer [$ defui]]
             [antd :refer [Button Flex Input InputNumber Form Spin Switch]]
             ["@ant-design/icons" :refer [MinusCircleOutlined]]
             [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
             [pigeon-scoops.controls.ingredients-selector :refer [ingredients-selector]]
             [pigeon-scoops.hooks :refer [use-recipe]]))

(def TextArea (.-TextArea Input))

(defn on-finish [values]
  (prn "Submit:" values))

(defui recipe-form [{:keys [recipe-id]}]
  (let [{:keys [recipe loading?]} (use-recipe recipe-id)
        [form] (Form.useForm)
        mystery? (Form.useWatch ":recipe/is-mystery" form)]
    (if (or loading? (not recipe))
      ($ Spin)
      ($ Form {:form form :on-finish on-finish :initial-values (clj->js recipe :keyword-fn str)}
         ($ Form.Item
            ($ Button {:type "primary" :html-type "submit"}
               (if recipe-id "Update Recipe" "Create Recipe")))
         ($ Form.Item {:label "Name" :name ":recipe/name" :rules (clj->js [{:required true}])}
            ($ Input))
         ($ Form.Item {:label "Public" :name ":recipe/public"}
            ($ Switch))
         ($ Form.Item {:label "Mystery Flavor" :name ":recipe/is-mystery"}
            ($ Switch))
         ($ Form.Item {:label "Source" :name ":recipe/source"}
            ($ Input))
         ($ Form.Item {:label "Description" :name ":recipe/description"}
            ($ TextArea))
         (when mystery?
           ($ Form.Item {:label "Mystery Description" :name ":recipe/mystery-description"}
              ($ TextArea)))
         ($ Flex {:direction "row"}
            ($ Form.Item {:label "Amount" :name ":recipe/amount" :rules (clj->js [{:required true}])}
               ($ InputNumber))
            ($ constants-selector {:form-item-name ":recipe/amount-unit" :constants-key :constants/unit-types :required? true}))
         ($ Form.List {:name ":recipe/ingredients"}
            (fn [fields funcs]
              ($ :div
                 (for [field fields]
                   (let [{:keys [key] field-name :name} (js->clj field :keywordize-keys true)
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)]
                     ($ Flex {:direction "row" :key key}
                        ($ Form.Item
                           ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)})))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Ingredient")))))))))
