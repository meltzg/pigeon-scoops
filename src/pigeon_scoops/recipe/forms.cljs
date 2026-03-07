(ns pigeon-scoops.recipe.forms
  (:require
   ["@ant-design/icons" :refer [ExportOutlined MinusCircleOutlined]]
   [antd :refer [Button Flex Form Input InputNumber Space Spin Switch Tabs]]
   [clojure.string :as str]
   [pigeon-scoops.api :refer [base-url]]
   [pigeon-scoops.components.bom-table :refer [bom-view]]
   [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
   [pigeon-scoops.controls.ingredients-selector :refer [ingredient->option
                                                        ingredients-selector
                                                        parse-ingredient]]
   [pigeon-scoops.fetchers :refer [delete-fetcher! post-fetcher! put-fetcher!]]
   [pigeon-scoops.hooks :refer [invalidate-recipes use-recipe use-recipe-bom
                                use-token]]
   [pigeon-scoops.utils :refer [determine-ops parse-keyword stringify-keyword]]
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
      ((partial parse-ingredient
                :ingredient/ingredient-id
                {:grocery :ingredient/ingredient-grocery-id
                 :recipe :ingredient/ingredient-recipe-id}))))

(defn recipe-form-values->data [form-values]
  (-> form-values
      (js->clj :keywordize-keys true)
      (update :recipe/instructions #(cond
                                      (coll? %) %
                                      (> (count %) 0) (str/split % #"\n")
                                      :else []))
      (update :recipe/amount-unit parse-keyword)
      (update :recipe/ingredients
              (fn [ingredients]
                (map ingredient-form-values->data
                     ingredients)))))

(defn ingredient->comparable
  ([ingredient]
   (ingredient->comparable ingredient []))
  ([ingredient additional-keys]
   (-> ingredient
       (ingredient-form-values->data)
       (select-keys (concat [:ingredient/amount
                             :ingredient/amount-unit
                             :ingredient/ingredient-grocery-id
                             :ingredient/ingredient-recipe-id]
                            additional-keys)))))

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

(defn on-finish [initial-recipe token values]
  (let [recipe (recipe-form-values->data values)
        recipe-id (atom (:recipe/id recipe))
        ingredient-ops (determine-ops :ingredient/id
                                      (:recipe/ingredients initial-recipe)
                                      (:recipe/ingredients recipe)
                                      #(ingredient->comparable % [:ingredient/id]))
        headers {"Content-Type" "application/transit+json"}]
    (-> (if (nil? @recipe-id)
          (-> (post-fetcher!
               (str base-url "/recipes")
               {:token token
                :body (->> recipe
                           (remove #(nil? (second %)))
                           (into {}))
                :headers headers})
              (.then #(do
                        (reset! recipe-id (:id %))
                        (rfe/push-state :pigeon-scoops.recipe.routes/recipe
                                        {:recipe-id @recipe-id}))))
          (put-fetcher! (str base-url "/recipes/" @recipe-id) {:token token :body recipe :headers headers}))
        (.then (fn [_]
                 (js/Promise.all (clj->js (concat
                                           (map #(post-fetcher! (str base-url "/recipes/" @recipe-id "/ingredients")
                                                                {:token token :body % :headers headers})
                                                (:new ingredient-ops))
                                           (map #(put-fetcher! (str base-url "/recipes/" @recipe-id "/ingredients")
                                                               {:token token :body % :headers headers}) (:update ingredient-ops))
                                           (map #(delete-fetcher! (str base-url "/recipes/" @recipe-id "/ingredients")
                                                                  {:token token :body {:ingredient/id %} :headers headers}) (:delete ingredient-ops)))))))
        (.then #(invalidate-recipes))
        (.catch (fn [e]
                  (js/alert (str "Error saving recipe: " (.-message e))))))))

(defn on-delete [token recipe-id]
  (-> (delete-fetcher! (str base-url "/recipes/" recipe-id) {:token token})
      (.then (fn [_]
               (invalidate-recipes)
               (rfe/push-state :pigeon-scoops.recipe.routes/recipes)))
      (.catch (fn [error]
                (js/alert (str "Error deleting recipe: " (.-message error)))))))

(defui recipe-form [{:keys [recipe-id scaled-amount scaled-amount-unit]}]
  (let [{:keys [token]} (use-token)
        {:keys [recipe loading?]} (use-recipe recipe-id scaled-amount scaled-amount-unit)
        {:keys [groceries]} (use-recipe-bom recipe-id
                                            (or scaled-amount
                                                (:recipe/amount recipe))
                                            (or scaled-amount-unit
                                                (:recipe/amount-unit recipe)))
        [form] (Form.useForm)
        [initial-values set-initial-values!] (uix/use-state nil)
        [scaled-amount set-scaled-amount!] (uix/use-state scaled-amount)
        [scaled-amount-unit set-scaled-amount-unit!] (uix/use-state scaled-amount-unit)
        [unsaved-changes? set-unsaved-changes!] (uix/use-state false)
        mystery? (Form.useWatch "recipe/is-mystery" form)
        amount-unit-type (Form.useWatch "recipe/amount-unit" form)
        all-values (Form.useWatch nil form)]

    (uix/use-effect
     (fn []
       (when recipe
         (let [form-values (recipe-data->form-values recipe)]
           (.setFieldsValue form (clj->js form-values :keyword-fn stringify-keyword))
           (set-initial-values! form-values))))
     [form recipe])

    (uix/use-effect
     (fn []
       (set-unsaved-changes! (apply not= (map recipe->comparable [recipe all-values]))))
     [recipe all-values])

    (if (or loading? (and (not= recipe-id :new) (not (uuid? recipe-id))))
      ($ Spin)
      ($ Form {:form form
               :on-finish (partial on-finish recipe token all-values)
               :style {:width "100%"}
               :disabled scaled-amount
               :initial-values (clj->js initial-values :keyword-fn stringify-keyword)}
         ($ Space {:align "start"}
            ($ Button {:html-type "button"
                       :disabled unsaved-changes?
                       :on-click #(rfe/push-state :pigeon-scoops.recipe.routes/recipes)} "Return to Recipes")
            ($ Button {:type "primary" :html-type "submit" :disabled (not unsaved-changes?)}
               (if (uuid? recipe-id) "Update Recipe" "Create Recipe"))
            ($ Button {:html-type "button" :on-click #(.resetFields form)} "Reset")
            ($ Button {:html-type "button" :danger true :on-click (partial on-delete token recipe-id)} "Delete")
            ($ InputNumber {:placeholder "Scale Amount"
                            :value scaled-amount
                            :disabled false
                            :on-change set-scaled-amount!})
            ($ constants-selector {:constants-key :constants/unit-types
                                   :value scaled-amount-unit
                                   :on-change set-scaled-amount-unit!
                                   :disabled? false
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
         ($ Form.Item {:label "Source" :name (stringify-keyword :recipe/source) :rules (clj->js [{:required true}])}
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
         ($ Tabs {:items
                  (clj->js
                   [{:label "Ingredients"
                     :key :ingredient
                     :children
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
                                    ($ ingredients-selector {:form-item-name (clj->js [field-name (stringify-keyword :ingredient/ingredient-id)])
                                                             :ingredient-keys {:grocery :ingredient/ingredient-grocery-id
                                                                               :recipe :ingredient/ingredient-recipe-id}})
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
                                ($ Button {:type "dashed" :on-click (:add (js->clj funcs :keywordize-keys true))} "Add Ingredient")))))}
                    {:label "Bill of Materials"
                     :key :bom
                     :children ($ bom-view {:groceries groceries})}])})))))
