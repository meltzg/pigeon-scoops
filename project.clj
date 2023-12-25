(defproject pigeon-scoops "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-codec "1.2.0"]
                 [ring-logger "1.1.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [metosin/muuntaja "0.6.8"]

                 ;; Logging
                 [ch.qos.logback/logback-classic "1.4.11"]
                 [ch.qos.logback/logback-core "1.4.11"]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [org.slf4j/jcl-over-slf4j "2.0.9"]
                 [org.slf4j/log4j-over-slf4j "2.0.9"]
                 [org.slf4j/osgi-over-slf4j "2.0.9"]
                 [org.slf4j/jul-to-slf4j "2.0.9"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.20.0"]

                 ;; CLJS Dependencies
                 [thheller/shadow-cljs "2.25.8"]
                 [rum "0.12.11"]
                 [cljs-ajax "0.8.4"]]
  :main ^:skip-aot pigeon-scoops.core
  :target-path "target/%s"
  :source-paths ["src/clj"
                 "src/cljs"]
  :profiles {:uberjar {:aot :all}})
