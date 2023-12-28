(ns pigeon-scoops.utils
  (:require [clojure.string :as string]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/v1/"))
