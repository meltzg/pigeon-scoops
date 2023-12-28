(ns pigeon-scoops.core
  (:require [ajax.core :as a]
            [pigeon-scoops.apps.groceries :refer [grocery-list]]
            [pigeon-scoops.utils :refer [api-url]]
            [uix.core :as uix :refer [defui $]]
            [uix.dom]))

(defui content []
       (let [[groceries set-groceries!] (uix/use-state nil)]
         (uix/use-effect
           (fn []
             (a/GET (str api-url "groceries")
                    {:response-format :transit
                     :keywords?       true?
                     :handler         set-groceries!}))
           [])
         ($ grocery-list {:groceries groceries})))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ content) root))

(defn ^:export init []
  (render))
