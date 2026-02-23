(ns pigeon-scoops.recipe.forms
  (:require  [uix.core :as uix :refer [$ defui]]
             [antd :refer [Button Flex Input InputNumber Form Spin Switch]]
             [pigeon-scoops.controls.constants-selector :refer [constants-selector]]
             [pigeon-scoops.controls.ingredients-selector :refer [ingredients-selector]]
             [pigeon-scoops.hooks :refer [use-recipe]]
             [pigeon-scoops.utils :refer [deep-stringify-keyword-vals]]))

(def TextArea (.-TextArea Input))

(defn on-finish [values]
  (prn "Submit:" values))

(defui recipe-form [{:keys [recipe-id]}]
  (let [{:keys [recipe loading?]} (use-recipe recipe-id)]
    (if (or loading? (not recipe))
      ($ Spin)
      ($ Form {:on-finish on-finish :initial-values (clj->js (update-keys (deep-stringify-keyword-vals recipe) str))}
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
         ($ Form.Item {:label "Mystery Description" :name ":recipe/mystery-description"}
            ($ TextArea))
         ($ Flex {:direction "row"}
            ($ Form.Item {:label "Amount" :name ":recipe/amount" :rules (clj->js [{:required true}])}
               ($ InputNumber))
            ($ constants-selector {:form-item-name ":recipe/amount-unit" :constants-key :constants/unit-types :required? true}))))))
