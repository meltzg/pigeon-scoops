(ns fiddle
  (:require [com.stuartsierra.component :as component]
            (pigeon-scoops.components
              [config-manager :as cm]
              [db :as db]
              [auth-manager :as am])))

(defn new-system
  ([] (new-system nil))
  ([app-settings-file]
   (component/system-map
     :config-manager (cm/make-config-manager app-settings-file)
     :database (component/using
                 (db/make-database)
                 [:config-manager])
     :auth-manager (component/using
                     (am/make-auth-manager)
                     [:database]))))

(def auth-sys (new-system))
