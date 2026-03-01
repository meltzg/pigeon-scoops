(ns pigeon-scoops.recipe.forms
  (:require
   ["@ant-design/icons" :refer [ExportOutlined MinusCircleOutlined]]
   [antd :refer [Button Flex Form Input InputNumber Space Spin Switch]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as str]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredient->option
                                                        ingredients-selector
                                                        parse-ingredient]]
   [pigeon-scoops.hooks :refer [use-recipe]]
   [pigeon-scoops.utils :refer [parse-keyword stringify-keyword]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(def TextArea (.-TextArea Input))

(defn recipe-data->form-values [data]
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

(defn ingredient-form-values->data [form-value]
  (-> form-value
      (js->clj :keywordize-keys true)
      (update :ingredient/amount-unit parse-keyword)
      (parse-ingredient)))

(defn recipe-form-values->data [form-values]
  (-> form-values
      (js->clj :keywordize-keys true)
      (update :recipe/instructions #(cond
                                      (coll? %) %
                                      (> (count %) 0)(str/split % #"\n")
                                      :else []))
      (update :recipe/amount-unit parse-keyword)
      (update :recipe/ingredients
              (fn [ingredients]
                (map ingredient-form-values->data
                     ingredients)))))

(defn ingredient->comparable [ingredient]
  (-> ingredient
      (ingredient-form-values->data)
      (select-keys [:ingredient/amount
                    :ingredient/amount-unit
                    :ingredient/ingredient-grocery-id
                    :ingredient/ingredient-recipe-id])))

(defn recipe->comparable [recipe]
  (-> recipe
      (recipe-form-values->data)
      (select-keys [:recipe/name
                    :recipe/public
                    :recipe/is-mystery
                    :recipe/source
                    :recipe/description
                    :recipe/mystery-description
                    :recipe/amount
                    :recipe/amount-unit
                    :recipe/instructions
                    :recipe/ingredients])
      (update :recipe/ingredients #(map ingredient->comparable %))))

(defn on-finish [values]
  (prn "Submit:")
  (pprint (recipe-form-values->data values)))

(defui recipe-form [{:keys [recipe-id scaled-amount scaled-amount-unit]}]
  (let [{:keys [recipe loading?]} (use-recipe recipe-id scaled-amount scaled-amount-unit)
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [scaled-amount set-scaled-amount!] (uix/use-state scaled-amount)
        [scaled-amount-unit set-scaled-amount-unit!] (uix/use-state scaled-amount-unit)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        mystery? (Form.useWatch "recipe/is-mystery" form)
        amount-unit-type (Form.useWatch "recipe/amount-unit" form)
        all-values (Form.useWatch nil form)]
    
    (prn "scaled-amount" scaled-amount "scaled-amount-unit" scaled-amount-unit)

    (uix/use-effect
     (fn []
       (when recipe
         (let [form-values (recipe-data->form-values recipe)]
           (.setFieldsValue form (clj->js form-values :keyword-fn str))
           (set-initial-values! form-values))))
     [form recipe])

    (uix/use-effect
     (fn []
       (set-unsaved-changes! (apply not= (map recipe->comparable [recipe all-values]))))
     [recipe all-values])

    (if (or loading? (not recipe))
      ($ Spin)
      ($ Form {:form form :on-finish on-finish :style {:width "100%"} :initial-values (clj->js initial-values :keyword-fn str)}
         ($ Space {:align "start"}
            ($ Button {:type "primary" :html-type "submit" :disabled (not unsaved-changes?)}
               (if recipe-id "Update Recipe" "Create Recipe"))
            ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")
            ($ InputNumber {:placeholder "Scale Amount"
                            :value scaled-amount
                            :on-change set-scaled-amount!})
            ($ constants-selector {:constants-key :constants/unit-types
                                   :value scaled-amount-unit
                                   :on-change set-scaled-amount-unit!
                                   :valid-namespaces (when amount-unit-type [(keyword (namespace (parse-keyword amount-unit-type)))])})
            ($ Button {:html-type "button"
                       :disabled (or (nil? scaled-amount) (nil? scaled-amount-unit))
                       :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipe
                                                  {:recipe-id (:recipe/id recipe)}
                                                  {:amount scaled-amount :amount-unit scaled-amount-unit})}
               "Scale Recipe")
            ($ Button {:html-type "button"
                       :disabled (nil? scaled-amount)
                       :on-click #(do
                                    (set-scaled-amount! nil)
                                    (rfe/push-state :pigeon-scoops.recipe.routes/recipe
                                                    {:recipe-id (:recipe/id recipe)}))}
               "Reset Scaling"))
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
                         {:keys [remove]} (js->clj funcs :keywordize-keys true)
                         ingredient (get-in (js->clj (.getFieldsValue form (clj->js [[(stringify-keyword :recipe/ingredients) field-name]]))
                                                     :keywordize-keys true)
                                            [:recipe/ingredients field-name])
                         parsed-ingredient (when (:ingredient/ingredient-id ingredient)
                                             (ingredient-form-values->data ingredient))]
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
                           ($ Button {:type "text"
                                      :disabled (or (nil? (:ingredient/id parsed-ingredient))
                                                    unsaved-changes?)
                                      :icon ($ ExportOutlined)
                                      :on-click #(apply rfe/push-state
                                                        (if (:ingredient/ingredient-recipe-id parsed-ingredient)
                                                          [:pigeon-scoops.recipe.routes/recipe
                                                           {:recipe-id (:ingredient/ingredient-recipe-id parsed-ingredient)}
                                                           {:amount      (:ingredient/amount parsed-ingredient)
                                                            :amount-unit (:ingredient/amount-unit parsed-ingredient)}]
                                                          [:pigeon-scoops.grocery.routes/grocery
                                                           {:grocery-id (:ingredient/ingredient-grocery-id parsed-ingredient)}]))}))
                        ($ Form.Item
                           ($ Button {:type "text" :danger true :icon ($ MinusCircleOutlined) :on-click #(remove field-name)})))))
                 ($ Form.Item
                    ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Ingredient")))))))))
