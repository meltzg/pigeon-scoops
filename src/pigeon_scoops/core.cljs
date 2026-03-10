(ns pigeon-scoops.core
  (:require
   ["@auth0/auth0-react" :refer [Auth0Provider]]
   ["react-icons/gi" :refer [GiIceCreamCone]]
   ["react-icons/io5" :refer [IoReceiptOutline]]
   ["react-icons/pi" :refer [PiCookingPot]]
   ["react-icons/fa" :refer [FaMoon FaSun]]
   ["@ant-design/icons" :refer [HomeOutlined ShoppingCartOutlined]]
   [antd :refer [ConfigProvider Dropdown Flex Layout Menu Space Switch Tooltip Typography theme]]
   [pigeon-scoops.auth :refer [authenticator]]
   [pigeon-scoops.router :refer [router-context with-router]]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def Header (.-Header Layout))
(def Content (.-Content Layout))
(def Sider (.-Sider Layout))

(def menu-on-clicks
  {:home #(rfe/push-state :pigeon-scoops.router/root)
   :recipes #(rfe/push-state :pigeon-scoops.recipe.routes/recipes)
   :groceries #(rfe/push-state :pigeon-scoops.grocery.routes/groceries)
   :orders #(rfe/push-state :pigeon-scoops.user-order.routes/orders)})

(def menu-items [{:key :home
                  :icon ($ HomeOutlined)
                  :label "Home"}
                 {:key :recipes
                  :icon ($ PiCookingPot)
                  :label "Recipes"}
                 {:key :groceries
                  :icon ($ ShoppingCartOutlined)
                  :label "Groceries"}
                 {:key :orders
                  :icon ($ IoReceiptOutline)
                  :label "Orders"}])

(defui content []
  (let [{:keys [route]} (uix/use-context router-context)
        [light-theme? set-light-theme!] (uix/use-state false)
        set-light-theme! (fn [is-light?]
                           (js/localStorage.setItem "light-theme?" is-light?)
                           (set-light-theme! is-light?))
        [prefer-sys-theme? set-prefer-sys-theme!] (uix/use-state false)
        set-prefer-sys-theme! (fn [prefer?]
                                (js/localStorage.setItem "prefer-system-theme?" prefer?)
                                (set-prefer-sys-theme! prefer?))]

    (uix/use-effect
     (fn []
       (let [stored-theme (js/localStorage.getItem "light-theme?")
             stored-prefer-sys-theme (js/localStorage.getItem "prefer-system-theme?")]

         (when stored-theme
           (set-light-theme! (= stored-theme "true")))
         (when stored-prefer-sys-theme
           (set-prefer-sys-theme! (= stored-prefer-sys-theme "true")))))
     [])

    (uix/use-effect
     (fn []
       (let [mq (js/window.matchMedia "(prefers-color-scheme: light)")
             on-change (fn [_]
                         (when prefer-sys-theme?
                           (set-light-theme! (.-matches mq))))]
         (when prefer-sys-theme?
           (set-light-theme! (.-matches mq)))
         (.addEventListener mq "change" on-change)
         (fn []
           (.removeEventListener mq "change" on-change))))
     [prefer-sys-theme?])

    ($ ConfigProvider {:theme (clj->js {:algorithm
                                        (if light-theme?
                                          (.-defaultAlgorithm theme)
                                          (.-darkAlgorithm theme))})}
       (prn light-theme?)
       ($ Layout {:style {:min-height "100vh"}}
          ($ Header
             ($ Flex {:justify "space-between" :align "center" :style {:height "100%"}}
                ($ Typography.Title {:level 3 :style {:color "white"}}
                   ($ GiIceCreamCone)
                   "Pigeon Scoops Manager")
                ($ Space
                   ($ Dropdown {:trigger (clj->js ["contextMenu"])
                                :menu
                                (clj->js
                                 {:items [{:key :prefer-system-theme
                                           :label ($ Space
                                                     "Prefer system thme"
                                                     ($ Switch {:checked prefer-sys-theme?
                                                                :on-change #(set-prefer-sys-theme! %)}))}]})}
                      ($ Tooltip {:title "Toggle light/dark theme.\nRight click for more options."}
                         ($ Switch {:checked light-theme?
                                    :on-change (when-not prefer-sys-theme?
                                                 #(set-light-theme! %))
                                    :checked-children ($ FaSun)
                                    :un-checked-children ($ FaMoon)})))
                   ($ authenticator))))
          ($ Layout
             ($ Sider {:collapsible true
                       :breakpoint "lg"}
                ($ Menu {:items (clj->js menu-items)
                         :mode "inline"
                         :theme "dark"
                         :on-click (fn [e] (((keyword (.-key e)) menu-on-clicks)))}))
             ($ Content {:style {:margin "0.5rem"}}
                (when route
                  ($ (-> route :data :view) (:parameters route)))))))))

(defui app []
  ($ Auth0Provider {:domain               "pigeon-scoops.us.auth0.com"
                    :client-id            "AoU9LnGWQlCbSUvjgXdHf4NZPJh0VHYD"
                    :cache-location       "localstorage"
                    :use-refresh-tokens   true
                    :authorization-params (clj->js {:redirect_uri (.. js/window -location -origin)
                                                    :scope        "openid profile email offline_access"
                                                    :audience     "https://api.pigeon-scoops.com"})}
     ($ with-router
        ($ content))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))

(defn render []
  (uix.dom/render-root ($ app) root))

(defn ^:export init []
  (render))
