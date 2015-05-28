(ns component-clinic.core-test
  (:require [clojure.test :refer :all]
            [component-clinic.core :refer :all]
            [com.stuartsierra.component :as component]
            ))

(defrecord Unstable []
  component/Lifecycle
  (start [this]
    (prn :starting-everything)
    (-> this (assoc :health (atom nil))
        attend!))

  (stop [this]
    (prn :stopping-everything)
    (discharge! this))

  curable
  (treat! [this]
    (prn :treating!!!)
    (cond
     (not @(:health this)) (reset! (:health this) 1)
     :else                 (swap!  (:health this) inc)))

  diagnosable
  (healthy? [this]
    (prn :how-are-you-feeling-today?)
    (prn this)
    (let [conn @(:health this)]
      (and conn (> conn 3)))))

(defn swait [this]
  (Thread/sleep (apply get-time [5 :seconds]))
  (prn this)
  (component/stop this))

(deftest cc-test
  (testing "Should run Component Clinic okay"
    (let [result (-> {:checkup-interval [1 :seconds]}
                     map->Unstable
                     component/start
                     swait
                     )]
      (is (> @(:health result) 3)))))

