(ns pigeon-scoops.components.api
  (:require [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes
                                    GET
                                    PATCH
                                    POST
                                    PUT
                                    DELETE]]
            [compojure.route :as route]
            [muuntaja.middleware :as mw]
            [jdbc-ring-session.core :as jdbc-ring-session]
            [ring.middleware.session :as ring-session]
            [pigeon-scoops.components
             [config-manager :as cm]
             [flavor-manager :as fm]
             [grocery-manager :as gm]
             [order-manager :as om]
             [recipe-manager :as rm]
             [auth-manager :as am]
             [db :as db]]
            [pigeon-scoops.spec.recipes :as rs]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.spec.orders :as os]
            [pigeon-scoops.spec.groceries :as gs]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :as log-mw]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as resp])
  (:import [java.util UUID]))

(defn auth-middleware [handler-fn]
  (fn [request]
    (let [session (:session request)]
      (if (seq session)
        (handler-fn request)
        (resp/status 401)))))

(defn get-groceries-handler [{:keys [grocery-manager]} params]
  (fn [& _]
    (resp/response (apply (partial gm/get-groceries! grocery-manager)
                          (map #(keyword (namespace ::gs/type) %)
                               (if (or (nil? (:types params)) (coll? (:types params)))
                                 (:types params)
                                 [(:types params)]))))))

(defn add-grocery-item-handler [{:keys [grocery-manager]} update?]
  (fn [{:keys [body-params]}]
    (let [updated-groceries (gm/add-grocery-item! grocery-manager body-params update?)]
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

(defn delete-grocery-item-handler [{:keys [grocery-manager]}]
  (fn [{:keys [body-params]}]
    (gm/delete-grocery-item! grocery-manager (:id body-params))
    (resp/status 204)))

(defn get-recipes-handler [{:keys [recipe-manager]} params]
  (fn [& _]
    (resp/response (apply (partial rm/get-recipes! recipe-manager)
                          (map parse-uuid
                               (if (or (nil? (:ids params)) (coll? (:ids params)))
                                 (:ids params)
                                 [(:ids params)]))))))

(defn add-recipe-handler [{:keys [recipe-manager]} update?]
  (fn [{:keys [body-params]}]
    (let [updated-recipe (rm/add-recipe! recipe-manager body-params update?)]
      (cond (nil? updated-recipe)
            (if update?
              (resp/not-found (str "No recipe item with id " (::rs/id body-params)))
              (-> (str "Recipe with ID " (::rs/id body-params) " already exists")
                  resp/bad-request
                  (resp/status 409)))
            (:clojure.spec.alpha/problems updated-recipe)
            (-> updated-recipe
                resp/bad-request
                (resp/status 422))
            :else
            (resp/response updated-recipe)))))

(defn delete-recipe-handler [{:keys [recipe-manager]}]
  (fn [{:keys [body-params]}]
    (rm/delete-recipe! recipe-manager (:id body-params))
    (resp/status 204)))

(defn get-flavors-handler [{:keys [flavor-manager]} params]
  (fn [& _]
    (resp/response (apply (partial fm/get-flavors! flavor-manager)
                          (map parse-uuid
                               (if (or (nil? (:ids params)) (coll? (:ids params)))
                                 (:ids params)
                                 [(:ids params)]))))))

(defn add-flavor-handler [{:keys [flavor-manager]} update?]
  (fn [{:keys [body-params]}]
    (let [updated-flavor (fm/add-flavor! flavor-manager body-params update?)]
      (cond (nil? updated-flavor)
            (if update?
              (resp/not-found (str "No flavor item with id " (::fs/id body-params)))
              (-> (str "flavor with ID " (::fs/id body-params) " already exists")
                  resp/bad-request
                  (resp/status 409)))
            (:clojure.spec.alpha/problems updated-flavor)
            (-> updated-flavor
                resp/bad-request
                (resp/status 422))
            :else
            (resp/response updated-flavor)))))

(defn delete-flavor-handler [{:keys [flavor-manager]}]
  (fn [{:keys [body-params]}]
    (fm/delete-flavor! flavor-manager (:id body-params))
    (resp/status 204)))

(defn get-scaled-flavor-recipes-handler [{:keys [flavor-manager]} {:keys [id amount amount-unit]}]
  (fn [& _]
    (resp/response (as-> (UUID/fromString id) acc
                         (fm/get-flavors! flavor-manager acc)
                         (first acc)
                         (fm/scale-flavor acc (Double/parseDouble amount) (keyword amount-unit))
                         (fm/materialize-recipes! flavor-manager acc)))))

(defn get-orders-handler [{:keys [order-manager]} params]
  (fn [& _]
    (resp/response (apply (partial om/get-orders! order-manager)
                          (map parse-uuid
                               (if (or (nil? (:ids params)) (coll? (:ids params)))
                                 (:ids params)
                                 [(:ids params)]))))))

(defn add-order-handler [{:keys [order-manager]} update?]
  (fn [{:keys [body-params]}]
    (let [updated-order (om/add-order! order-manager body-params update?)]
      (cond (nil? updated-order)
            (if update?
              (resp/not-found (str "No order item with id " (::os/id body-params)))
              (-> (str "order with ID " (::os/id body-params) " already exists")
                  resp/bad-request
                  (resp/status 409)))
            (:clojure.spec.alpha/problems updated-order)
            (-> updated-order
                resp/bad-request
                (resp/status 422))
            :else
            (resp/response updated-order)))))

(defn delete-order-handler [{:keys [order-manager]}]
  (fn [{:keys [body-params]}]
    (om/delete-order! order-manager (:id body-params))
    (resp/status 204)))

(defn get-order-groceries-handler [{:keys [order-manager flavor-manager grocery-manager]} {:keys [id]}]
  (fn [& _]
    (let [recipe-ingredients (->> (UUID/fromString id)
                                  (om/get-orders! order-manager)
                                  first
                                  ::os/flavors
                                  (map #(assoc % ::os/recipes (as-> % acc
                                                                    (::os/flavor-id acc)
                                                                    (fm/get-flavors! flavor-manager acc)
                                                                    (first acc)
                                                                    (fm/scale-flavor acc (::os/amount %) (::os/amount-unit %))
                                                                    (fm/materialize-recipes! flavor-manager acc))))
                                  (mapcat ::os/recipes)
                                  (rm/merge-recipe-ingredients))
          groceries (apply (partial gm/get-groceries! grocery-manager) (map ::rs/ingredient-type recipe-ingredients))]
      (resp/response (rm/to-grocery-purchase-list recipe-ingredients groceries)))))

(defn check-sign-in-handler [session]
  (if (seq session)
    (resp/status 200)
    (resp/status 401)))

(defn sign-in-handler [{:keys [auth-manager]}]
  (fn [{:keys [body-params]}]
    (let [{:keys [email password]} body-params
          [account valid?] (am/sign-in! auth-manager email password)]
      (if (and account valid?)
        (-> (resp/status 200)
            (assoc :session account))
        (-> (resp/status 401))))))

(defn sign-out-handler []
  (fn [_]
    (-> (resp/status 200)
        (assoc :session nil))))

(defn app-routes [api]
  (routes
    (GET "/" {} (resp/resource-response "index.html" {:root "public"}))
    (GET "/api/v1/signIn" {session :session} (check-sign-in-handler session))
    (POST "/api/v1/signIn" {} (sign-in-handler api))
    (POST "/api/v1/signOut" {} (sign-out-handler))
    (GET "/api/v1/groceries" {params :params} (auth-middleware (get-groceries-handler api params)))
    (PUT "/api/v1/groceries" {} (auth-middleware (add-grocery-item-handler api false)))
    (PATCH "/api/v1/groceries" {} (auth-middleware (add-grocery-item-handler api true)))
    (DELETE "/api/v1/groceries" {} (auth-middleware (delete-grocery-item-handler api)))
    (GET "/api/v1/recipes" {params :params} (auth-middleware (get-recipes-handler api params)))
    (PUT "/api/v1/recipes" {} (auth-middleware (add-recipe-handler api false)))
    (PATCH "/api/v1/recipes" {} (auth-middleware (add-recipe-handler api true)))
    (DELETE "/api/v1/recipes" {} (auth-middleware (delete-recipe-handler api)))
    (GET "/api/v1/flavors" {params :params} (auth-middleware (get-flavors-handler api params)))
    (PUT "/api/v1/flavors" {} (auth-middleware (add-flavor-handler api false)))
    (PATCH "/api/v1/flavors" {} (auth-middleware (add-flavor-handler api true)))
    (DELETE "/api/v1/flavors" {} (auth-middleware (delete-flavor-handler api)))
    (GET "/api/v1/flavors/:id/recipes" {params :params} (auth-middleware (get-scaled-flavor-recipes-handler api params)))
    (GET "/api/v1/orders" {params :params} (auth-middleware (get-orders-handler api params)))
    (PUT "/api/v1/orders" {} (auth-middleware (add-order-handler api false)))
    (PATCH "/api/v1/orders" {} (auth-middleware (add-order-handler api true)))
    (DELETE "/api/v1/orders" {} (auth-middleware (delete-order-handler api)))
    (GET "/api/v1/orders/:id/groceries" {params :params} (auth-middleware (get-order-groceries-handler api params)))
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config-manager database auth-manager grocery-manager recipe-manager flavor-manager order-manager]
  component/Lifecycle

  (start [this]
    (let [{::cm/keys [app-host app-port]} (::cm/app-settings config-manager)]
      (logger/info (str "Starting server on host " app-host " port: " app-port))
      (assoc this :server (run-jetty
                            (-> (app-routes this)
                                log-mw/wrap-with-logger
                                mw/wrap-format
                                (wrap-keyword-params {:parse-namespaces? true})
                                wrap-params
                                wrap-nested-params
                                (ring-session/wrap-session {:store (jdbc-ring-session/jdbc-store
                                                                     (::db/spec database)
                                                                     {:table :sessions})}))
                            {:host  app-host
                             :port  app-port
                             :join? false}))))

  (stop [this]
    (logger/log :info "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
