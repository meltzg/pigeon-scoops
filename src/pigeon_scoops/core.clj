(ns pigeon-scoops.core
  (:require (pigeon-scoops
              [api :as api]
              [config-manager :as cm])
            [com.stuartsierra.component :as component])
  (:gen-class))


(defn new-system [app-settings-file]
  (component/system-map
    :config-manager (cm/make-config-manager app-settings-file)
    :api (component/using
           (api/make-api)
           [:config-manager])))

(defn -main
  [& args]
  (let [system (component/start (new-system (first args)))
        lock (promise)
        stop (fn []
               (component/stop system)
               (deliver lock :release))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop))
    @lock
    (System/exit 0)))
