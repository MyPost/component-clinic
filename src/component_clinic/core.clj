(ns component-clinic.core
  "
  Component-Clinic
  ================

  PSA: Your components may be sick!
       Make sure they are having regular checkups at the component-clinic!

  > A small helper library to allow components to be made healthy.

  > Useful for treating components that may become diseased on-the-fly.

  > Initialize sickly components to facilitate crash-driven development.

  > Works well with Stuart Sierra components, but doesn't depend on them.

  ---

  We provide two protocols, and two functions:

  * curable      (treat!   this)
  * diagnosable  (healthy? this)
  * attend!
  * discharge!

  ... respectively

  This library is intended to act on a patient (object), who at the bare minimum:

  * Implements curable
  * Is at some-point declared under care through the use of 'attend

  If you do this, then your patient will be checked periodically
  (defaulting to 10 :seconds) to see if they are healthy (defaulting to sick)
  and if found unwell, will be healed using your definition of 'treat!
  in the implementation of the curable protocol.

  If you wish to provide your own diagnostics, then you may also implement
  the diagnosable protocol (with its 'healthy? method) in order to
  test well-being. A falsy response will indicate that the patient is sick.

  The interval of diagnosis repetition is provided as an option
  to 'attend as :checkup-interval. If this option is missing, then
  the same key of your patient will be used.
  Finally, if that key is missing too, then the interval will default to
  10 seconds. Intervals are indicated either with milliseconds,
  or with a tuple of [magnitude, units] (see 'get-time).

  If you wish to discharge your patient, then you may do so explicitly
  with the 'discharge method. This is recommended in favor of simply
  losing their records.

  The first checkup/treatment of 'attend! is synchronous in order
  for component dependency management to be made as transparent
  as possible. If you wish to skip this initial step, then you
  may set the :skip-initial-checkup? option of attend! to true.


  Example:

    (require '[com.stuartsierra.component :as component])
    (require '[component-clinic.core      :refer :all])

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
      (component/stop this))

    (-> {:checkup-interval [1 :seconds]}
        map->Unstable
        component/start
        swait)
  "
  (:require [taoensso.timbre :as timbre]))


(defprotocol curable
  "
  The ability to treat a patient (AKA, repair an object).
  "
  (treat! [this]
    "
    Perform stateful actions to restore the health of your patient.
    "))

(defprotocol diagnosable
  "
  The ability to diagnose a patient (AKA, health-check an object).
  "
  (healthy? [this]
    "
    Check the health of your patient.

    Falsey values (nil, false) indicate that your patient is sick.

    Truthy values indicate that they are well.
    "))

;; Implementation Details

;; Thanks StackOverflow!
;; http://stackoverflow.com/questions/6694530/executing-a-function-with-a-timeout
(defmacro with-timeout [millis & body]
  `(let [future# (future ~@body)]
     (try
       (.get future# ~millis java.util.concurrent.TimeUnit/MILLISECONDS)
       (catch java.util.concurrent.TimeoutException x#
         (do
           (future-cancel future#)
           nil)))))

(defn get-unit
  "
  Translate a unit of time (represented by a symbol) into milliseconds.

  Example: (get-unit :seconds) ;=> 1000

  Supported units:

    :ms     :millisecond :milliseconds
    :second :seconds
    :minute :minutes
    :hour   :hours
    :day    :days
  "
  [units]
  (condp = units
    :ms           1
    :millisecond  1
    :milliseconds 1
    :second       1000
    :seconds      1000
    :minute       (* 60 1000)
    :minutes      (* 60 1000)
    :hour         (* 60 60 1000)
    :hours        (* 60 60 1000)
    :day          (* 24 60 60 1000)
    :days         (* 24 60 60 1000)))

(defn get-time
  "
  Get the time in milliseconds from either [magnitude, units],
  or [milliseconds].

  Example: (get-time 10 :seconds) ;=> 10000
  "
  ([mag units] (* mag (get-unit units)))
  ([mag] mag))

(defn every
  "
  Perform an action every delta-time-period.

  Actions that take longer than delta will be killed.

  Returns a future in order to allow the repetition to be cancelled.
  "
  [delta fun & args]
  (let [delta-ms (apply get-time delta)]
    (future
      (while true
        (let [before (System/currentTimeMillis)]
          (with-timeout delta-ms
            (try (apply fun args)
                 (catch Exception e (timbre/error e "Caught exception during 'every")))
            (let [transpired (- (System/currentTimeMillis) before)]
              (if (< transpired delta-ms)
                (Thread/sleep (- delta-ms transpired))))))))))

(defn checkup!
  "
  Checks the health of your patient, and treats them if they are sick.

  If your object doesn't implement 'healthy? then this will default to sick.
  "
  [this]
  (timbre/tracef "Checking %s" (class this))
  (if (satisfies? diagnosable this)
    (let [well? (healthy? this)]
      (if well?
        (timbre/tracef "No issues with %s" (class this))
        (do (timbre/infof "Treating %s" (class this))
            (treat! this)
            (let [well-well? (healthy? this)]
              (if-not (= well? well-well?)
                (timbre/warnf "Recovered %s. healthy=%s -> healthy=%s"
                              (class this) well? well-well?))))))
    (do (timbre/infof "Patient [%s] cannot be checked (consider implementing diagnosable). Running 'treat!." (class this))
        (treat! this))))

(defn initial-checkup! [this]
  (try
    (checkup! this)
    (catch Exception e
      (timbre/errorf e "Severe sickness found during admittance of [%s]... Will continue background checks." (class this)))))

(def attend-defaults {:checkup-interval      [10 :seconds]
                      :skip-initial-checkup? false })

(defn attend!
  "
  Look after a patient.

  Takes the options in this, and in a second options map.

  Default options are:

  { :skip-initial-checkup? false
    :checkup-interval      [10 :seconds]
  }
  "
  ([this] (attend! this {}))
  ([this options]
     (assert (satisfies? curable this))
     (let [opts                  (merge attend-defaults this options)
           skip-initial-checkup? (:skip-initial-checkup? opts)
           interval              (:checkup-interval      opts)]
       (timbre/infof "Attending patient [%s]" (class this))
       (if-not skip-initial-checkup? (initial-checkup! this))
       (assoc this ::attending
              (every interval checkup! this)))))

(defn discharge!
  "
  Stop attending to a patient.

  TODO: The printing of ::attending may cause issues if it has been cancelled.
        Consider wrapping it in a non-printing record to stop this issue.
  "
  [this]
  (timbre/infof "Discharging patient [%s]" (class this))
  (future-cancel (::attending this))
  (dissoc this ::attending))
