(ns pigeon-scoops.core
  (:require [com.stuartsierra.component :as component]
            (pigeon-scoops.components
              [api :as api]
              [config-manager :as cm]
              [db :as db]
              [flavor-manager :as fm]
              [grocery-manager :as gm]
              [order-manager :as om]
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
                       [:database :grocery-manager])
     :flavor-manager (component/using
                       (fm/make-flavor-manager)
                       [:database :recipe-manager])
     :order-manager (component/using
                      (om/make-flavor-manager)
                      [:database :grocery-manager :recipe-manager :flavor-manager])
     :api (component/using
            (api/make-api)
            [:config-manager :grocery-manager :recipe-manager :flavor-manager :order-manager]))))

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
