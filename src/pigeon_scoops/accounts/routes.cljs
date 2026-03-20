(ns pigeon-scoops.accounts.routes
  (:require [pigeon-scoops.accounts.views :refer [accounts-table]]))

(def routes ["/accounts"
             {:name ::accounts
              :view accounts-table}])
