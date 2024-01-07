(ns pigeon-scoops.components.db
  (:require [com.stuartsierra.component :as component]
            [pigeon-scoops.components.config-manager :as cm]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defrecord DataBase [config-manager]
  component/Lifecycle

  (start [this]
    (let [[db_url
           db_host
           db_port
           db_name
           db_user
           db_password] ((juxt ::cm/db_url
                               ::cm/db_host
                               ::cm/db_port
                               ::cm/db_name
                               ::cm/db_user
                               ::cm/db_password) (::cm/app-settings config-manager))
          db_spec (jdbc/get-datasource (or db_url
                                           (format "jdbc:postgresql://%s:%d/%s?user=%s&password=%s"
                                                   db_host
                                                   db_port
                                                   db_name
                                                   db_user
                                                   db_password)))]
      (println db_spec)
      (assoc this ::connection (jdbc/get-connection db_spec))))

  (stop [this]
    (.close (::connection this))
    (assoc this ::connection nil)))

(defn make-database []
  (map->DataBase {}))
