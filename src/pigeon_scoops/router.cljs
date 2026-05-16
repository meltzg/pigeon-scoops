(ns pigeon-scoops.router
  (:require [pigeon-scoops.accounts.routes :as accounts]
            [pigeon-scoops.grocery.routes :as grocery]
            [pigeon-scoops.menu.routes :as menu]
            [pigeon-scoops.recipe.routes :as recipe]
            [pigeon-scoops.storefront.forms :refer [storefront-form]]
            [pigeon-scoops.user-order.routes :as order]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [uix.core :refer [$ defui] :as uix]))

(def router-context (uix/create-context))

(def routes
  [["/" {:name ::root
         :view storefront-form}]
   grocery/routes
   order/routes
   recipe/routes
   menu/routes
   accounts/routes])

(defui with-router [{:keys [children]}]
  (let [router (uix/use-memo #(rf/router routes {:data {:coercion rss/coercion}}) [routes])
        [route set-route] (uix/use-state nil)]

    (uix/use-effect
     #(rfe/start! router set-route {:use-fragment false})
     [router])

    ($ (.-Provider router-context) {:value {:route route}}
       children)))
