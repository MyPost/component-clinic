(defproject au.com.auspost/component-clinic "0.1.0-SNAPSHOT"

  :description "Look after the health of your components at the component-clinic! "

  :url "https://github.com/MyPost/component-clinic"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 ]

  :plugins [[org.clojars.cvillecsteele/lein-git-version "1.0.1"]]

  :git-version-path "src/component_clinic"

  :repositories {"clojars-https" {:url "https://clojars.org/au.com.auspost"
                                  :username "sordina"
                                  :password :env
                                  :sign-releases false }}

  :profiles {:test {:dependencies [[com.stuartsierra/component "0.2.1"]]}})
