(defproject au.com.auspost/component-clinic "0.1.0-SNAPSHOT"

  :description "Look after the health of your components at the component-clinic! "

  :url "https://github.com/MyPost/component-clinic"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 ]

  :profiles {:test {:dependencies [[com.stuartsierra/component "0.2.1"]]}})
