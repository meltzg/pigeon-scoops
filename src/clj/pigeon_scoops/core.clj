(ns pigeon-scoops.core
  (:require [com.stuartsierra.component :as component]
            (pigeon-scoops.components
              [api :as api]
              [config-manager :as cm]
              [db :as db]
              [grocery-manager :as gm]
              [recipe-manager :as rm]))
  (:gen-class))


(defn new-system
  ([] (new-system nil))
  ([app-settings-file]
   (component/system-map
     :config-manager (cm/make-config-manager app-settings-file)
     :database (component/using
                 (db/make-database)
                 [:config-manager])
     :grocery-manager (component/using
                        (gm/make-grocery-manager)
                        [:database])
     :recipe-manager (component/using
                       (rm/make-recipe-manager)
                       [:config-manager :grocery-manager])
     :api (component/using
            (api/make-api)
            [:config-manager :grocery-manager :recipe-manager]))))

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
