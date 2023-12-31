(ns pigeon-scoops.core
  (:require [com.stuartsierra.component :as component]
            (pigeon-scoops.components
              [api :as api]
              [config-manager :as cm]
              [grocery-manager :as gm]
              [recipe-manager :as rm]))
  (:gen-class))


(defn new-system
  ([] (new-system nil))
  ([app-settings-file]
   (component/system-map
     :config-manager (cm/make-config-manager app-settings-file)
     :grocery-manager (component/using
                        (gm/make-grocery-manager)
                        [:config-manager])
     :recipe-manager (component/using
                       (rm/make-recipe-manager)
                       [:config-manager :grocery-manager])
     :api (component/using
            (api/make-api)
            [:config-manager :grocery-manager]))))

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
