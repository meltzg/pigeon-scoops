(ns pigeon-scoops.controls.ingredients-selector
  (:require
   [antd :refer [Form Select Spin]]
   [clojure.string :as str]
   [pigeon-scoops.components.select-options-sizer :refer [select-options-sizer]]
   [pigeon-scoops.hooks :refer [use-groceries use-recipes]]
   [uix.core :as uix :refer [$ defui]]))

(defn parse-ingredient [ingredient-id-key ingredient-keys ingredient]
  (let [[type id] (when (ingredient-id-key ingredient)
                    (.split (ingredient-id-key ingredient) ":"))
        type (keyword type)]
    (cond
      (nil? type) ingredient
      (= type :grocery) (assoc ingredient (:grocery ingredient-keys) (uuid id))
      (= type :recipe) (assoc ingredient (:recipe ingredient-keys) (uuid id))
      :else (throw (ex-info "Invalid ingredient type" {:type type})))))

(defn stringify-ingredient [type id]
  (str (name type) ":" id))

(defn ingredient->option [ingredient-keys ingredient]
  (cond
    ((:recipe ingredient-keys) ingredient)
    (stringify-ingredient :recipe ((:recipe ingredient-keys) ingredient))
    ((:grocery ingredient-keys) ingredient)
    (stringify-ingredient :grocery ((:grocery ingredient-keys) ingredient))))

(defui ingredients-selector [{:keys [form-item-name ingredient-keys]}]
  (let [{:keys [groceries] groceries-loading? :loading?} (use-groceries)
        {:keys [recipes] recipes-loading? :loading?} (use-recipes)
        valid-ingredients (set (keys ingredient-keys))
        recipes (apply concat (vals recipes))
        [options set-options!] (uix/use-state [])
        [select-width set-select-width!] (uix/use-state "auto")]
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
      ($ :div
         ($ select-options-sizer {:options options
                                  :on-size-change set-select-width!})
         ($ Form.Item {:name form-item-name
                       :rules (clj->js [{:required true}])}
            ($ Select {:show-search (clj->js {:optionFilterProp :label})
                       :style (clj->js {:width select-width})
                       :options (clj->js options)}))))))
