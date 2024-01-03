(ns pigeon-scoops.components.api
  (:require [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes
                                    GET
                                    PATCH
                                    PUT
                                    DELETE]]
            [compojure.route :as route]
            [muuntaja.middleware :as mw]
            [pigeon-scoops.components.config-manager :as cm]
            [pigeon-scoops.components.grocery-manager :as gm]
            [pigeon-scoops.components.recipe-manager :as rm]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.groceries :as gs]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :as log-mw]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as resp]))

(defn get-groceries-handler [grocery-manager params]
  (fn [& _]
    (resp/response (apply (partial gm/get-groceries grocery-manager)
                          (map #(keyword (namespace ::gs/type) %)
                               (if (or (nil? (:types params)) (coll? (:types params)))
                                 (:types params)
                                 [(:types params)]))))))

(defn add-grocery-item-handler [grocery-manager update?]
  (fn [{:keys [body-params]}]
    (let [updated-groceries (gm/add-grocery-item grocery-manager body-params update?)]
      (cond (nil? updated-groceries)
            (if update?
              (resp/not-found (str "No grocery item with type " (::gs/type body-params)))
              (-> (str "Grocery item with type " (::gs/type body-params) " already exists")
                  resp/bad-request
                  (resp/status 409)))
            (:clojure.spec.alpha/problems updated-groceries)
            (-> updated-groceries
                resp/bad-request
                (resp/status 422))
            :else
            (resp/response updated-groceries)))))

(defn delete-grocery-item-handler [grocery-manager]
  (fn [{:keys [body-params]}]
    (gm/delete-grocery-item grocery-manager
                            (keyword (namespace ::gs/type) (:type body-params)))
    (resp/status 204)))

(defn get-recipes-handler [recipe-manager params]
  (fn [& _]
    (resp/response (apply (partial rm/get-recipes recipe-manager)
                          (map parse-uuid
                               (if (or (nil? (:ids params)) (coll? (:ids params)))
                                 (:ids params)
                                 [(:ids params)]))))))

(defn add-recipe-handler [recipe-manager update?]
  (fn [{:keys [body-params]}]
    (let [updated-recipes (rm/add-recipe recipe-manager body-params update?)]
      (cond (nil? updated-recipes)
            (if update?
              (resp/not-found (str "No recipe item with id " (::rs/id body-params)))
              (-> (str "Recipe with ID " (::rs/id body-params) " already exists")
                  resp/bad-request
                  (resp/status 409)))
            (:clojure.spec.alpha/problems updated-recipes)
            (-> updated-recipes
                resp/bad-request
                (resp/status 422))
            :else
            (resp/response updated-recipes)))))

(defn delete-recipe-handler [recipe-manager]
  (fn [{:keys [body-params]}]
    (rm/delete-recipe recipe-manager (:id body-params))
    (resp/status 204)))

(defn app-routes [grocery-manager recipe-manager]
  (routes
    (GET "/" {} (resp/resource-response "index.html" {:root "public"}))
    (GET "/api/v1/groceries" {params :params} (get-groceries-handler grocery-manager params))
    (PUT "/api/v1/groceries" {} (add-grocery-item-handler grocery-manager false))
    (PATCH "/api/v1/groceries" {} (add-grocery-item-handler grocery-manager true))
    (DELETE "/api/v1/groceries" {} (delete-grocery-item-handler grocery-manager))
    (GET "/api/v1/recipes" {params :params} (get-recipes-handler recipe-manager params))
    (PUT "/api/v1/recipes" {} (add-recipe-handler recipe-manager false))
    (PATCH "/api/v1/recipes" {} (add-recipe-handler recipe-manager true))
    (DELETE "/api/v1/recipes" {} (delete-recipe-handler recipe-manager))
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config-manager grocery-manager recipe-manager]
  component/Lifecycle

  (start [this]
    (let [{::cm/keys [app-host app-port]} (::cm/app-settings config-manager)]
      (logger/info (str "Starting server on host " app-host " port: " app-port))
      (assoc this :server (run-jetty
                            (-> (app-routes grocery-manager recipe-manager)
                                log-mw/wrap-with-logger
                                mw/wrap-format
                                (wrap-keyword-params {:parse-namespaces? true})
                                wrap-params
                                wrap-nested-params)
                            {:host  app-host
                             :port  app-port
                             :join? false}))))

  (stop [this]
    (logger/log :info "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
