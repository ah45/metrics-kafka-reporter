`metrics-kafka-reporter`
========================

A reporter for [Codahale/Dropwizard Metrics][metrics] that publishes
measurements to an [Apache Kafka][kafka] topic as JSON documents.

Please refer to the [main project `README`][prj-url] for an overview.

A [Clojure library][mkr-clj] is also available.

[prj-url]: https://github.com/ah45/metrics-kafka-reporter
[metrics]: https://dropwizard.github.io/metrics
[kafka]: http://kafka.apache.org/
[mkr-clj]: ../metrics-kafka-reporter-clj
[javadoc]: https://ah45.github.io/metrics-kafka-reporter/

## Installation

Available from [Clojars](https://clojars.org/metrics-kafka-reporter):

[![Clojars Project](http://clojars.org/metrics-kafka-reporter/latest-version.svg)](http://clojars.org/metrics-kafka-reporter)

### Gradle Dependency

    repositories {
        clojars {
            url "http://clojars.org/repo"
        }
    }

    compile "metrics-kafka-reporter:metrics-kafka-reporter:$version"

### Maven Dependency

    <repositories>
      <repository>
        <id>clojars.org</id>
        <url>http://clojars.org/repo</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>metrics-kafka-reporter</groupId>
      <artifactId>metrics-kafka-reporter</artifactId>
      <version>$version</version>
    </dependency>

## Using

The main `KafkaReporter` class implements the `ScheduledReporter`
interface and so follows the pattern of:

1. Instantiate a builder:

        KafkaReporter.forRegistry(registry)

2. Set build options:

        .convertRatesTo(TimeUnit.SECONDS)
        .filter(MetricFilter.ALL)

3. Build the reporter, providing a Kafka `Producer` instance:

        .build(producer);

Building the reporter has a single required argument: the Kafka
producer to use for sending messags. As with the other reporters all
of the build options are optional and have default values.

Please refer to the [Javadoc documentation][javadoc] for details of
the available build options and their defaults.

## Example

Post a metric containing a random number every 10 seconds:

```java
// imports
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.codahale.metrics.MetricRegistry;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;

import io.dropwizard.metrics.kafka.KafkaReporter;

// registry, connections, etc.
final MetricRegistry registry = new MetricRegistry();

Map<String,String> config = new HashMap<String, String>();

config.put("bootstrap.servers", "127.0.0.1:9092");

final StringSerializer serializer = new StringSerializer();
final KafkaProducer<String, String> kafka = new KafkaProducer<String, String>(config, serializer, serializer);

final KafkaReporter reporter = KafkaReporter.forRegistry(registry).build(kafka);

// metric
registry.register(MetricRegistry.name("metrics-kafka-reporter", "example", "random"),
                  new Gauge<Integer>() {
                      private final static Random rand = new Random();

                      @Override
                      public Integer getValue() {
                          return rand.nextInt(10000);
                      }
                  });

reporter.start(10, TimeUnit.SECONDS);
```

## Documentation

[Javadoc documentation][javadoc] is available and serves as the
primary reference.

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
