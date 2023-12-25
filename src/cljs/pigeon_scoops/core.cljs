(ns pigeon-scoops.core
  (:require
    [uix.core :refer [defui $]]
    [uix.dom]))

(defui content []
  ($ :div {} "Hello world!"))

(defonce root
         (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ content) root))

(defn ^:export init []
  (render))
