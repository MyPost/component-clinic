(defproject au.com.auspost/component-clinic "SNAPSHOT"

  :description "Look after the health of your components at the component-clinic! "

  :url "https://github.com/MyPost/component-clinic"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 ]

  :plugins [[lein-version-spec "0.0.4"]]

  :version-spec "~{:env/project_version}"

  :repositories {"clojars-https" {:url "http://clojars.org/repo"
                                  :username "sordina"
                                  :password :env
                                  :sign-releases false }}

  :profiles {:test {:dependencies [[com.stuartsierra/component "0.2.1"]]}})
