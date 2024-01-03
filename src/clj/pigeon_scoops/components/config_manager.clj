(ns pigeon-scoops.components.config-manager
  (:require [clojure.edn :as edn]
            [clojure.java.io :refer [as-file]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.spec.basic :as bs]))

(s/def ::app-host ::bs/non-empty-string)
(s/def ::app-port pos?)
(s/def ::groceries-file ::bs/non-empty-string)
(s/def ::recipes-file ::bs/non-empty-string)
(s/def ::app-settings (s/keys :req [::app-host
                                    ::app-port
                                    ::groceries-file
                                    ::recipes-file]))

(def env-defaults
  {::app-host       (or (System/getenv "PIGEON_HOST") "0.0.0.0")
   ::app-port       (Integer/parseInt (or (System/getenv "PIGEON_PORT") "8080"))
   ::groceries-file (or (System/getenv "PIGEON_GROCERIES") "resources/groceries.edn")
   ::recipes-file   (or (System/getenv "PIGEON_RECIPES") "resources/recipes.edn")})

(defrecord ConfigManager [app-settings-file]
  component/Lifecycle

  (start [this]
    (let [app-settings (merge env-defaults
                              (when (and (some? app-settings-file)
                                         (.exists (as-file app-settings-file)))
                                (-> app-settings-file
                                    slurp
                                    edn/read-string)))]
      (if (s/valid? ::app-settings app-settings)
        (do
          (logger/info (str "Loaded settings " app-settings))
          (assoc this ::app-settings app-settings))
        (throw (ex-info "Invalid app settings" (s/explain ::app-settings app-settings))))))

  (stop [this]
    (assoc this ::app-settings nil)))

(defn make-config-manager [app-settings-file]
  (map->ConfigManager {:app-settings-file app-settings-file}))
