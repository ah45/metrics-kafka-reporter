`metrics-kafka-reporter`
========================

A reporter for [Codahale/Dropwizard Metrics][metrics] that publishes
measurements to an [Apache Kafka][kafka] topic as JSON documents.

Both [Java][mkr-java] and [Clojure][mkr-clj] reporters are available:

[![Clojars Project](http://clojars.org/metrics-kafka-reporter/latest-version.svg)](http://clojars.org/metrics-kafka-reporter)
[![Clojars Project](http://clojars.org/metrics-kafka-reporter/reporter-clj/latest-version.svg)](http://clojars.org/metrics-kafka-reporter/reporter-clj)

[metrics]: https://dropwizard.github.io/metrics
[kafka]: http://kafka.apache.org/
[`metrics-clojure`]: http://metrics-clojure.readthedocs.org/

[mkr-java]: ./metrics-kafka-reporter
[mkr-clj]: ./metrics-kafka-reporter-clj

## Compatibility

This reporter has been tested against:

* [Metrics][metrics] version `3.1.0`
* [Kafka][kafka] version `0.8.2.2`
* [`metrics-clojure`] version `2.5.1`

It should be compatible with any API compatible release of the above.

## Message Format

By default one message is sent to Kafka per metric with the key set to
the metric name and the message itself containing a JSON
representation of the metric object.

The JSON serialization is provided by the
[metrics library][metrics-json] itself. For details you are best off
referring to the [source][metrics-json-src]. In addition to the metric
properties a `timestamp` field is written to the JSON object providing
an ISO 8601 formatted string of the date/time the metric was
calculated.

Using [Metrics][metrics] version `3.1.0` some examples of the produced
message values are:

```json
// Gauge
{"value": 3, "timestamp": "2015-10-22T11:50:34.762Z"}

// Counter
{"count": 7, "timestamp": "2015-10-22T11:50:34.762Z"}

// Meter
{
  "count": 7,
  "m1_rate": 2,
  "m5_rate": 4,
  "m15_rate": 3,
  "mean_rate": 1,
  "units": "events/second",
  "timestamp": "2015-10-22T11:50:34.762Z"
}

// Histogram
{
  "count": 7,
  "max": 2,
  "mean": 4,
  "min": 3,
  "p50": 1,
  ... p75, p95, p98, p99 ...
  "p999": 1,
  "stddev": "0.013",
  "timestamp": "2015-10-22T11:50:34.762Z"
}
```

[metrics-json]: http://metrics.dropwizard.io/3.1.0/manual/json/
[metrics-json-src]: https://github.com/dropwizard/metrics/blob/master/metrics-json/src/main/java/io/dropwizard/metrics/json/MetricsModule.java

However, the format and number of messages is entirely configurable by
providing your own metric serializer. You can format the metrics as
plain text, [`msgpack`], or any other format you care to as well as
bundling all metrics into a single message or grouping them by
topic—all by just swapping out the
[metric → Kafka serializer][serializer].

[`msgpack`]: http://msgpack.org/
[serializer]: ./metrics-kafka-reporter/src/main/java/io/dropwizard/metrics/kafka/serialization/KafkaMetricsSerializer.java

## Installation & Use

Please refer to the sub-projects for installation and usage guidelines
for your environment:

* [Java][mkr-java]
* [Clojure][mkr-clj]

## Gotchas

There are two main potential pitfalls when setting up the
communication with Kafka:

* Using the “old” style producer

  The reporter can only be used with the “new” (as of version
  `0.8.2.0`) Kafka Java producers—those from the
  `org.apache.kafka.clients.producer` namespace—and not the “old”
  Scala producers—those from the `kafka.javaapi.producer` namespace.
* Using incompatible serializers

  The messages being sent to Kafka must be serializable by the
  producer, seems obvious but its easy to end up with a mismatch and
  the only way you'll know is by seeing lots of errors reported in the
  log.

  By default the reporter produces messages that have `String` keys
  and values. This will work fine if you are using the
  `StringSerializer` for both.

  If you are using the `ByteArraySerializer` then you'll need to
  change the `serializer` used by the reporter to the included
  `JSONByteArraySerializer`.

  If you are using some combination of serializers (e.g. strings for
  keys, bytes for values) or a custom serializer then you will need to
  [provide your own serializer][serializer] for the reporter to use.

  Overriding the `JSONStringSerializer` to produce messages with a
  different key/value serialization only requires implementing one
  method. This is what the included [`JSONByteArraySerializer`] does.

[`JSONByteArraySerializer`]: ./metrics-kafka-reporter/src/main/java/io/dropwizard/metrics/kafka/serialization/JSONByteArraySerializer.java

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
