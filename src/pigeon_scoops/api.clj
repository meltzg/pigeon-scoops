(ns pigeon-scoops.api
  (:require [com.stuartsierra.component :as component]))

(defrecord Api [config-manager]
  component/Lifecycle

  (start [this]
    (let [{:config-manager/keys [app-host app-port]} (:app-settings config-manager)]
      (println app-host app-port)))

  (stop [this]))

(defn make-api []
  (map->Api {}))
