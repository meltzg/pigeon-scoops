(ns pigeon-scoops.controls.ingredients-selector
  (:require [uix.core :as uix :refer [$ defui]]
            [antd :refer [Form Select Spin]]
            [pigeon-scoops.hooks :refer [use-groceries use-recipes]]
            [clojure.string :as str]))

(defn parse-ingredient [value]
  (let [[type id] (.split value ":")]
    {:type (keyword type)
     :id id}))

(defn stringify-ingredient [type id]
  (str (name type) ":" id))

(defn ingredient->option [ingredient]
  (cond
    (:ingredient/ingredient-recipe-id ingredient)
    (stringify-ingredient :recipe (:ingredient/ingredient-recipe-id ingredient))
    (:ingredient/ingredient-grocery-id ingredient)
    (stringify-ingredient :grocery (:ingredient/ingredient-grocery-id ingredient))))

(defui ingredients-selector [{:keys [form-item-name valid-ingredients]}]
  (let [{:keys [groceries] groceries-loading? :loading?} (use-groceries)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        valid-ingredients (set (if (seq valid-ingredients)
                                 valid-ingredients
                                 [:grocery :recipe]))
        recipes (apply concat (vals recipes))
        [options set-options!] (uix/use-state [])]
    (uix/use-effect
     (fn []
       (when-not (or groceries-loading? recipes-loading?)
         (set-options! (sort-by (comp str/lower-case :label)
                                (concat
                                 (when (valid-ingredients :grocery)
                                   (for [g groceries]
                                     {:value (stringify-ingredient :grocery (:grocery/id g))
                                      :label (:grocery/name g)}))
                                 (when (valid-ingredients :recipe)
                                   (for [r recipes]
                                     {:value (stringify-ingredient :recipe (:recipe/id r))
                                      :label (:recipe/name r)})))))))
     [valid-ingredients groceries recipes groceries-loading? recipes-loading?])
    (if (or groceries-loading? recipes-loading?)
      ($ Spin)
      ($ Form.Item {:name form-item-name
                    :rules (clj->js [{:required true}])}
         ($ Select {:show-search (clj->js {:optionFilterProp :label})
                    :options (clj->js options)})))))
