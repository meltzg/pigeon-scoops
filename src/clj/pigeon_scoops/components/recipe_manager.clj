(ns pigeon-scoops.components.recipe-manager
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.components.config-manager :as cm]
            [pigeon-scoops.units.common :as units]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs])
  (:import (java.util UUID)))

(defrecord RecipeManager [config-manager grocery-manager]
  component/Lifecycle

  (start [this]
    (assoc this ::recipes (-> config-manager
                              ::cm/app-settings
                              ::cm/recipes-file
                              slurp
                              edn/read-string
                              atom)))

  (stop [this]
    (logger/info "Saving changes to groceries")
    (spit (-> config-manager ::cm/app-settings ::cm/recipes-file)
          (with-out-str (clojure.pprint/pprint (deref (::recipes this)))))
    (assoc this ::recipes nil)))

(defn make-recipe-manager []
  (map->RecipeManager {}))

(defn get-recipes [recipe-manager & ids]
  (cond->> (deref (::recipes recipe-manager))
           (not-empty ids) (filter #(some #{(::rs/id %)} ids))))

(defn add-recipe [recipes new-recipe]
  (let [recipe-id (or (::rs/id new-recipe)
                      (UUID/randomUUID))
        conformed-recipe (s/conform ::rs/entry (assoc new-recipe ::rs/id recipe-id))]
    (if (s/invalid? conformed-recipe)
      recipes
      (conj (remove #(= (::rs/id %) recipe-id) recipes) conformed-recipe))))

(defn scale-recipe [recipe amount amount-unit]
  (let [scale-factor (units/scale-factor (::rs/amount recipe)
                                         (::rs/amount-unit recipe)
                                         amount
                                         amount-unit)]
    (-> recipe
        (update ::rs/ingredients (partial map #(update % ::rs/amount * scale-factor)))
        (update ::rs/mixins (partial map #(update % ::rs/amount * scale-factor)))
        (assoc ::rs/amount amount
               ::rs/amount-unit amount-unit))))


(defn materialize-mixins [recipe-to-materialize recipe-library]
  (loop [to-materialize (::rs/mixins recipe-to-materialize)
         materialized-mixins []]
    (if (empty? to-materialize)
      materialized-mixins
      (let [mixin-recipes (map (fn [mixin]
                                 (scale-recipe (->> recipe-library
                                                    (filter #(= (::rs/id %) (::rs/id mixin)))
                                                    first)
                                               (::rs/amount mixin)
                                               (::rs/amount-unit mixin)))
                               to-materialize)
            new-mixins (mapcat ::rs/mixins mixin-recipes)]
        (recur new-mixins (concat materialized-mixins mixin-recipes))))))

(defn merge-recipe-ingredients [recipes-to-merge recipe-library]
  (->> (mapcat ::rs/ingredients recipes-to-merge)
       (concat (mapcat #(mapcat ::rs/ingredients (materialize-mixins % recipe-library)) recipes-to-merge))
       (group-by #(list (::rs/ingredient-type %) (namespace (::rs/amount-unit %))))
       vals
       (map #(reduce (fn [acc ingredient]
                       (update acc ::rs/amount + (units/convert (::rs/amount ingredient)
                                                                (::rs/amount-unit ingredient)
                                                                (::rs/amount-unit acc)))) %))))

(defn to-grocery-purchase-list [recipe-ingredients groceries]
  (let [grocery-map (into {} (map #(vec [(::gs/type %) %]) groceries))
        purchase-list (map #(gm/divide-grocery (::rs/amount %)
                                               (::rs/amount-unit %)
                                               ((::rs/ingredient-type %) grocery-map))
                           recipe-ingredients)]
    {:purchase-list purchase-list
     :total-cost    (apply + (map #(* (::gs/unit-cost %) (::gs/unit-purchase-quantity %))
                                  (mapcat ::gs/units purchase-list)))}))
