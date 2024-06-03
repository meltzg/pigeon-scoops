(ns pigeon-scoops.components.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as res]
            [pigeon-scoops.components.config-manager :as cm])
  (:import (java.sql Array)))


(extend-protocol res/ReadableColumn
  Array
  (read-column-by-label [^Array v _] (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))


(defmacro migrations [] (->> "resources/migrations/"
                             io/file
                             file-seq
                             (map #(.getName %))
                             (filter #(str/ends-with? % ".edn"))
                             sort
                             vec))

(defn- execute-migrations! [conn]
  (->> (migrations)
       (map (partial str "migrations/"))
       (map io/resource)
       (map slurp)
       (map edn/read-string)
       (map #(jdbc/with-transaction
               [conn conn]
               (doall (map (comp (partial jdbc/execute! conn)
                                 sql/format)
                           %))))
       doall
       ))

(defrecord DataBase [config-manager]
  component/Lifecycle

  (start [this]
    (let [db_spec (jdbc/get-datasource (or (-> config-manager ::cm/app-settings ::cm/db_url)
                                           (apply (partial format "jdbc:postgresql://%s:%d/%s?user=%s&password=%s")
                                                  ((juxt ::cm/db_host
                                                         ::cm/db_port
                                                         ::cm/db_name
                                                         ::cm/db_user
                                                         ::cm/db_password) (::cm/app-settings config-manager)))))
          conn (jdbc/get-connection db_spec)]
      (execute-migrations! conn)
      (assoc this ::connection conn
                  ::spec db_spec)))

  (stop [this]
    (.close (::connection this))
    (assoc this ::connection nil)))

(defn make-database []
  (map->DataBase {}))

(defn from-db-namespace [entity-spec entity]
  (->> (update-keys entity #(keyword (namespace entity-spec) (str/replace (name %) #"_" "-")))
       (filter #(some? (second %)))
       (into {})))
