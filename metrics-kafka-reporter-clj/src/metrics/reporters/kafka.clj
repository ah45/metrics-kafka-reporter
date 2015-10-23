(ns metrics.reporters.kafka
  (:import java.util.concurrent.TimeUnit
           [com.codahale.metrics
            MetricFilter
            MetricRegistry
            ScheduledReporter]
           io.dropwizard.metrics.kafka.KafkaReporter
           [io.dropwizard.metrics.kafka.serialization
            KafkaMetricsSerializer
            JSONStringSerializer
            JSONByteArraySerializer]
           org.apache.kafka.clients.producer.Producer))

(def string-serializer JSONStringSerializer/INSTANCE)
(def byte-array-serializer JSONByteArraySerializer/INSTANCE)

(defn ^io.dropwizard.metrics.kafka.KafkaReporter reporter
  ([^MetricRegistry reg ^Producer producer]
   (reporter reg producer {}))
  ([^MetricRegistry reg ^Producer producer opts]
   (let [b (KafkaReporter/forRegistry reg)]
     (when-let [^TimeUnit ru (:rate-unit opts)]
       (.convertRatesTo b ru))
     (when-let [^TimeUnit ru (:duration-unit opts)]
       (.convertDurationsTo b ru))
     (when-let [^MetricFilter f (:filter opts)]
       (.filter b f))
     (when-let [^String t (:topic opts)]
       (.topic b t))
     (when-let [^KafkaMetricsSerializer mf (:serializer opts)]
       (.serializer b mf))
     (.build b producer))))

(defn start
  [^ScheduledReporter r ^long seconds]
  (.start r seconds))

(defn stop
  [^ScheduledReporter r]
  (.stop r))
