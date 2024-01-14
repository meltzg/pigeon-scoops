(ns pigeon-scoops.components.db
  (:require [com.stuartsierra.component :as component]
            [pigeon-scoops.components.config-manager :as cm]
            [next.jdbc :as jdbc]
            [clojure.string :as str]
            [next.jdbc.result-set :as res])
  (:import [java.sql Array]))


(extend-protocol res/ReadableColumn
  Array
  (read-column-by-label [^Array v _] (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))

(defrecord DataBase [config-manager]
  component/Lifecycle

  (start [this]
    (let [db_spec (jdbc/get-datasource (or (-> config-manager ::cm/app-settings ::cm/db_url)
                                           (apply (partial format "jdbc:postgresql://%s:%d/%s?user=%s&password=%s")
                                                  ((juxt ::cm/db_host
                                                         ::cm/db_port
                                                         ::cm/db_name
                                                         ::cm/db_user
                                                         ::cm/db_password) (::cm/app-settings config-manager)))))]
      (assoc this ::connection (jdbc/get-connection db_spec))))

  (stop [this]
    (.close (::connection this))
    (assoc this ::connection nil)))

(defn make-database []
  (map->DataBase {}))

(defn from-db-namespace [entity-spec entity]
  (->> (update-keys entity #(keyword (namespace entity-spec) (str/replace (name %) #"_" "-")))
       (filter #(some? (second %)))
       (into {})))
