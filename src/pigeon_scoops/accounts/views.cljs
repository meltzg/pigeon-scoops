(ns pigeon-scoops.accounts.views
  (:require
   [antd :refer [Button Flex Select Spin Table]]
   [clojure.string :as str]
   [pigeon-scoops.fetchers :refer [put-fetcher!]]
   [pigeon-scoops.hooks :refer [base-url invalidate-accounts use-accounts
                                use-constants use-token]]
   [pigeon-scoops.utils.table :refer [make-filter make-sorter]]
   [pigeon-scoops.utils.transform :refer [stringify-keyword]]
   [uix.core :as uix :refer [$ defui]]))

(defui roles-selector [{:keys [account]}]
  (let [{:keys [constants loading?]} (use-constants)
        {:keys [token]} (use-token)
        [selected-roles set-selected-roles!] (uix/use-state (clj->js (:account/roles account)))
        actions-disabled? (= (set (js->clj selected-roles))
                             (set (:account/roles account)))]
    (if loading?
      ($ Spin)
      ($ Flex {:vertical false :wrap true}
         ($ Select {:mode "multiple"
                    :style {:width "100%"}
                    :placeholder "Select roles"
                    :value selected-roles
                    :options (clj->js (map (fn [role] {:label role :value role}) (:constants/roles constants)) :keyword-fn stringify-keyword)
                    :onChange set-selected-roles!})
         ($ Button {:type "primary"
                    :disabled actions-disabled?
                    :on-click #(-> (put-fetcher! (str base-url "/account/" (js/encodeURIComponent (:account/id account)))
                                                 {:token token
                                                  :headers {"Content-Type" "application/transit+json"}
                                                  :body {:roles (mapv keyword (js->clj selected-roles))}})
                                   (.then (fn [_] (invalidate-accounts))))}
            "Update")
         ($ Button {:disabled actions-disabled?
                    :on-click #(set-selected-roles! (clj->js (:account/roles account)))}
            "Reset")))))

(def columns
  [(merge {:title "Username"
           :dataIndex (stringify-keyword :account/name)
           :sorter (make-sorter :account/name)
           :key :name}
          (make-filter :account/name))
   {:title "Roles"
    :dataIndex (stringify-keyword :account/roles)
    :render (fn [_ record]
              (let [record (js->clj record :keywordize-keys true)]
                ($ roles-selector {:account record})))
    :key :roles}])

(defui accounts-table []
  (let [{:keys [accounts loading?]} (use-accounts)]
    (if loading?
      ($ Spin)
      ($ Table {:columns (clj->js columns)
                :dataSource (clj->js (map-indexed (fn [idx account] (assoc account :key idx))
                                                  (sort-by #(str/lower-case (:account/name %)) accounts))
                                     :keyword-fn stringify-keyword)}))))
