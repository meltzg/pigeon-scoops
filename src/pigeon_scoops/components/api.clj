(ns pigeon-scoops.components.api
  (:require [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [muuntaja.middleware :as mw]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :as log-mw]
            [ring.middleware.defaults :as defaults]))

(defn app-routes [config-manager]
  (routes
    (GET "api/v1/recipes" {} ())
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config-manager]
  component/Lifecycle

  (start [this]
    (let [{:config-manager/keys [app-host app-port]} (:app-settings config-manager)]
      (logger/info (str "Starting server on host " app-host " port: " app-port))
      (assoc this :server (run-jetty
                            (-> (app-routes config-manager)
                                log-mw/wrap-with-logger
                                mw/wrap-format
                                (defaults/wrap-defaults {:params {:keywordize true
                                                                  :urlencoded true}}))
                            {:host  app-host
                             :port  app-port
                             :join? false}))))

  (stop [this]
    (logger/log :info "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
