(ns pigeon-scoops.components.auth-manager
  (:require [com.stuartsierra.component :as component]
            [buddy.hashers :as bh]
            [honey.sql :as sql]
            [honey.sql.helpers :as hsql :refer [select
                                                from
                                                where
                                                delete-from
                                                insert-into
                                                values]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [pigeon-scoops.components.db :as db]))

(def create-accounts-table-statement {:create-table [:accounts :if-not-exists]
                                      :with-columns
                                      [[:email :text :primary-key [:not nil]]
                                       [:password :text [:not nil]]
                                       [:created-at :timestamp [:not nil] [:default :current-timestamp]]]})

(def create-sessions-table-statement {:create-table [:sessions :if-not-exists]
                                      :with-columns
                                      [[:session-id [:varchar 36] :primary-key [:not nil]]
                                       [:idle-timeout :bigint]
                                       [:absolute-timeout :bigint]
                                       [:value :bytea]]})

(defn sign-up! [auth-manager email password]
  (jdbc/execute-one! (-> auth-manager :database ::db/connection)
                     (-> (insert-into :accounts)
                         (values [{:email    email
                                   :password (bh/derive password)}])
                         (hsql/returning :email :created-at)
                         sql/format)
                     {:builder-fn rs/as-unqualified-kebab-maps}))

(defn sign-in! [auth-manager email password]
  (let [account (jdbc/execute-one! (-> auth-manager :database ::db/connection)
                                   (-> (select :*)
                                       (from :accounts)
                                       (where [:= :email email])
                                       sql/format)
                                   {:builder-fn rs/as-unqualified-kebab-maps})]
    [(select-keys account [:email :created-at]) (when account (:valid (bh/verify password (:password account))))]))

(defrecord AuthManager [database]
  component/Lifecycle

  (start [this]
    (jdbc/execute! (::db/connection database)
                   (sql/format create-accounts-table-statement))
    (jdbc/execute! (::db/connection database)
                   (sql/format create-sessions-table-statement))
    (assoc this ::auth {}))

  (stop [this]
    (assoc this ::auth nil)))

(defn make-auth-manager []
  (map->AuthManager {}))
