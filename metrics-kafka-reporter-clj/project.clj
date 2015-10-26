(defproject metrics-kafka-reporter/reporter-clj "0.1.0"
  :url "https://github.com/ah45/metrics-kafka-reporter"
  :description "A reporter for Codahale/Dropwizard Metrics that
    publishes measurements to an Apache Kafka topic as (by default)
    JSON documents."

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [metrics-kafka-reporter "0.1.0"]])
