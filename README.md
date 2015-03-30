Component-Clinic
================

[![Travis Status](https://travis-ci.org/MyPost/component-clinic.svg)](https://travis-ci.org/MyPost/component-clinic)

## PSA: Your components may be sick!

Make sure they are having regular checkups at the component-clinic!

* A small helper library to allow components to be made healthy.
* Useful for treating components that may become diseased on-the-fly.
* Initialize sickly components to facilitate crash-driven development.
* Works well with Stuart Sierra components, but doesn't depend on them.

## Usage

Leiningen:

		:dependencies [[au.com.auspost/component-clinic "0.1.0-SNAPSHOT"]]

In your code:

		(require '[component-clinic.core :as cc])

## Features

We provide two protocols (with one function each)

* curable      (treat!   this)
* diagnosable  (healthy? this)

and two additional functions

* attend!
* discharge!

... respectively.

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
