`metrics-kafka-reporter/reporter-clj`
=====================================

A [`metrics-clojure`] compatible reporter for
[Codahale/Dropwizard Metrics][metrics] that publishes measurements to
an [Apache Kafka][kafka] topic as JSON documents.

[metrics]: https://dropwizard.github.io/metrics
[kafka]: http://kafka.apache.org/
[`metrics-clojure`]: http://metrics-clojure.readthedocs.org/
[prj-url]: https://github.com/ah45/metrics-kafka-reporter

Please refer to the [main project `README`][prj-url] for an overview.

## Installation

Add the following dependency to your `project.clj`:

[![Clojars Project](http://clojars.org/metrics-kafka-reporter/reporter-clj/latest-version.svg)](http://clojars.org/metrics-kafka-reporter/reporter-clj)

Note that you will also need the [`metrics-clojure`] library and a
Kafka client library that provides instances of the “new” Kafka
producer class ([`clj-kafka`](https://clojars.org/clj-kafka) is
recommended.)

## Using

The interface is the same as the other
[`metrics-clojure` reporters](http://metrics-clojure.readthedocs.org/en/latest/reporting.html)
with one exception: you must specify the registry to report.

```clojure
(metrics.reporters.kafka/reporter
 metrics-registry
 kafka-producer
 {...options...})
```

Supported options are:

* `:rate-unit` – the unit in which metrics that report _rates_ are
  to be recorded.
* `:duration-unit` – the unit in which metrics that report _durations_
  are to be recorded.
* `:filter` – a `MetricFilter` to restrict the metrics that are
  reported.
* `:topic` – the name of the Kafka topic to which the metrics should
  be posted, defaults to “metrics”.
* `:serializer` – the `KafkaMetricSerializer` to use when serializing
  the metrics into messages to send to Kafka, defaults to the
  `JSONStringSerializer`.

The reporter can be started/stopped via the `metrics.reporters/start`
& `stop` methods (or the `start`/`stop` in the
`metrics.reporters.kafka` namespace):

```clojure
(metrics.reporters/start kafka-reporter 10) ; send metrics every 10 seconds
(metrics.reporters/stop kafka-reporter)
```

## Documentation

Please refer to the [Javadoc documentation][javadoc] of the underlying
Java classes for implementation details (this library is a _very_ thin
wrapper over them.)

[javadoc]: https://ah45.github.io/metrics-kafka-reporter/

## Example

The following example creates a rudimentary system with a single
metric (of a random number) being reported to Kafka every ten seconds.

```clojure
(ns metrics-kafka.example
  (:require [com.stuartsierra.component :as component]
            [clj-kafka.new.producer :as kafka]
            [metrics.core :as metrics]
            [metrics.gauges :as gauge]
            [metrics.reporters :as reporter]
            [metrics.reporters.kafka :as reporters.kafka]))

(defrecord Kafka [servers]
  component/Lifecycle
  (start [this]
    (if (:producer this)
      this
      (let [producer (kafka/producer
                      {"bootstrap.servers" servers}
                      (kafka/string-serializer)
                      (kafka/string-serializer))]
        (assoc this :producer producer))))
  (stop [this]
    (when-let [p (:producer this)] (.close p))
    (dissoc this :producer)))

(defrecord Metrics [kafka]
  component/Lifecycle
  (start [this]
    (if (:registry this)
      this
      (let [reg (metrics/new-registry)
            rep (reporters.kafka/reporter reg (:producer kafka))]
        (reporter/start rep 10) ; report every 10 seconds
        (assoc this :registry reg :reporter rep))))
  (stop [this]
    (when-let [r (:reporter this)] (reporter/stop r))
    (dissoc this :registry :reporter)))

(defrecord RandomMetric [metrics]
  component/Lifecycle
  (start [this]
    (if (:gauge this)
      this
      (assoc this :gauge (gauge/gauge-fn
                          (:registry metrics)
                          ["metrics-kafka" "example" "random"]
                          #(rand 10000)))))
  (stop [this]
    (when-let [g (:gauge this)]
      (metrics/remove-metric (:registry metrics) g))
    (dissoc this :gauge)))

(def system
  (component/system-map
   :kafka (->Kafka "192.168.99.100:39092")
   :metrics (component/using
             (map->Metrics {})
             [:kafka])
   :random-metric (component/using
                   (map->RandomMetric {})
                   [:metrics])))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(start)
```

## Using an Alternative Serializer

If you are using the `byte-array-serializer` in your Kafka producer:

```clojure
(kafka/producer
 {"bootstrap.servers" servers}
 (kafka/byte-array-serializer)
 (kafka/byte-array-serializer))
```

… then you'll also want to use the corresponding serializer when
creating the reporter:

```clojure
(reporters.kafka/reporter
 (metrics/default-registry)
 kafka-producer
 {:serializer reporter.kafka/byte-array-serializer})
```

For other serializers or combinations of `string-serializer` and
`byte-array-serializer` you will need to provide your own
implementation of `KafkaMetricSerializer`, or a subclass of
`JSONStringSerializer` that returns appropriately formatted messages
for Kafka as per `JSONByteArraySerializer`.

## License

Copyright © 2015 Adam Harper

Licensed under the Apache License, Version 2.0 (the “License”); you
may not use this file except in compliance with the License.  You may
obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an “AS IS” BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.
