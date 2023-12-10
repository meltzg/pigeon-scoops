(ns pigeon-scoops.config-manager
  (:require [clojure.edn :refer [read-string]]
            [clojure.java.io :refer [as-file]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [pigeon-scoops.basic-spec]))

(s/def :config-manager/app-host :basic-spec/non-empty-string)
(s/def :config-manager/app-port pos?)
(s/def :config-manager/app-settings (s/keys :req [:config-manager/app-host
                                                  :config-manager/app-port]))

(def env-defaults
  {:config-manager/app-host (or (System/getenv "PIGEON_HOST") "0.0.0.0")
   :config-manager/app-port (Integer/parseInt (or (System/getenv "PIGEON_PORT") "8080"))})

(defrecord ConfigManager [app-settings-file]
  component/Lifecycle

  (start [this]
    (let [app-settings (merge env-defaults
                              (if (and (some? app-settings-file)
                                       (.exists (as-file app-settings-file)))
                                (-> app-settings-file
                                    slurp
                                    read-string)))]
      (if (s/valid? :config-manager/app-settings app-settings)
        (do
          (logger/info (str "Loaded settings " app-settings))
          (assoc this :app-settings app-settings))
        (throw (ex-info "Invalid app settings" (s/explain :config-manager/app-settings app-settings))))))

  (stop [this]
    (assoc this :app-settings nil)))

(defn make-config-manager [app-settings-file]
  (map->ConfigManager {:app-settings-file app-settings-file}))
