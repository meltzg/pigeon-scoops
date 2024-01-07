(defproject pigeon-scoops "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.11.0"]
                 [ring/ring-jetty-adapter "1.11.0"]
                 [ring/ring-codec "1.2.0"]
                 [ring-logger "1.1.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [metosin/muuntaja "0.6.8"]
                 [com.github.seancorfield/honeysql "2.5.1103"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [org.postgresql/postgresql "42.7.1"]

                 ;; Logging
                 [ch.qos.logback/logback-classic "1.4.14"]
                 [ch.qos.logback/logback-core "1.4.14"]
                 [org.slf4j/slf4j-api "2.0.10"]
                 [org.slf4j/jcl-over-slf4j "2.0.10"]
                 [org.slf4j/log4j-over-slf4j "2.0.10"]
                 [org.slf4j/osgi-over-slf4j "2.0.10"]
                 [org.slf4j/jul-to-slf4j "2.0.10"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.22.1"]

                 ;; CLJS Dependencies
                 [thheller/shadow-cljs "2.26.2"]
                 [com.pitch/uix.core "1.0.1"]
                 [com.pitch/uix.dom "1.0.1"]
                 [cljs-ajax "0.8.4"]
                 [binaryage/devtools "1.0.7"]]
  :main ^:skip-aot pigeon-scoops.core
  :target-path "target/%s"
  :source-paths ["src/clj"
                 "src/cljc"
                 "src/cljs"
                 "dev"]
  :plugins [[com.github.clj-kondo/lein-clj-kondo "0.2.5"]
            [lein-ancient "0.7.0"]]
  :profiles {:uberjar {:aot :all}})
