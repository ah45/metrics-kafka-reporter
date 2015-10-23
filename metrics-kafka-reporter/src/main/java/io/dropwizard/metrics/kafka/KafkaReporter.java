package io.dropwizard.metrics.kafka;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import io.dropwizard.metrics.kafka.serialization.KafkaMetricsSerializer;
import io.dropwizard.metrics.kafka.serialization.JSONStringSerializer;

/**
 * A reporter which publishes metric values to an Apache Kafka Topic.
 *
 * <h2>Build Options</h2>
 * <ul>
 *   <li>{@code topic} &ndash; the topic to which the metrics will be
 *   published, defaults to {@value DEFAULT_TOPIC}
 *   </li>
 *
 *   <li>{@code serializer} &ndash; the {@link KafkaMetricsSerializer
 *   serializer} to use for converting metrics into messages to
 *   publish to Kafka. Defaults to the {@link JSONStringSerializer}
 *   which will convert each metric into a string encoded JSON message
 *   keyed by the metric name.
 *   </li>
 *
 *   <li>{@code filter} &ndash; a filter to use to restrict which
 *   metrics are published to Kafka. Defaults to no filter.
 *   </li>
 *
 *   <li>{@code rateUnit} &ndash; dictates the unit in which metrics
 *   that report <em>rates</em> are reported in. Defaults to seconds.
 *   </li>
 *
 *   <li>{@code durationUnit} &ndash; dictates the unit in which
 *   metrics that report <em>durations</em> are reported in. Defaults
 *   to milliseconds.
 *   </li>
 * </ul>
 *
 * <p>
 * <strong>Note:</strong> pay close attention to the {@code
 * serializer} the reporter is using! The messages ({@link
 * ProducerRecord} instances) it returns <em>must</em> by serializable
 * by your Kafka {@link Producer}. The default serializer produces
 * messages with {@code String} keys and values; if you are not using
 * the {@link org.apache.kafka.common.serialization.StringSerializer
 * StringSerializer} you <em>will</em> need to specify a different
 * serializer (e.g. the {@code JSONByteArraySerializer} if you are
 * using {@code ByteArraySerializer}.)
 * </p>
 *
 * <h2>Output</h2>
 * <p>
 * The exact format of the messages sent to Kafka is dependant upon
 * the {@link KafkaMetricsSerializer serializer} in use. By default
 * each metric is sent as its own message (that is: one message per
 * metric) with the key set to the metric name and the value a JSON
 * encoding of the metric properties (with an added timestamp.)
 * </p>
 *
 * <p>
 * See the {@link JSONStringSerializer} documentation for details of
 * the default message format used by this reporter.
 * </p>
 */
public final class KafkaReporter extends ScheduledReporter {
    public final static String DEFAULT_TOPIC = "metrics";

    /**
     * Returns a new {@link Builder} for {@link KafkaReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link KafkaReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link KafkaReporter} instances.
     */
    public static class Builder {
        private final MetricRegistry registry;

        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private String topic;
        private KafkaMetricsSerializer serializer;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;

            this.topic = DEFAULT_TOPIC;
            this.serializer = JSONStringSerializer.INSTANCE;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Post metrics to the given topic.
         *
         * Defaults to {@value #DEFAULT_TOPIC}.
         *
         * @param topic the name of the topic to post to
         * @return {@code this}
         */
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * Convert metrics to messages using the given serializer.
         *
         * <p>
         * The messages ({@link ProducerRecord} instances)
         * <strong>must</strong> be serializable by the {@link
         * Producer} you instantiate the reporter with.
         * </p>
         *
         * @param serializer a {@link KafkaMetricsSerializer}
         * @return {@code this}
         */
        public Builder serializer(KafkaMetricsSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        /**
         * Builds a {@link KafkaReporter} with the given properties,
         * sending metrics using the provided Kafka Java {@link
         * Producer} (Kafka versions 0.8.2 and later.)
         *
         * @param producer a Kafka {@link Producer}
         * @return a {@link KafkaReporter}
         */
        public KafkaReporter build(Producer<?,?> producer) {
            return new KafkaReporter(registry,
                                     producer,
                                     topic,
                                     serializer,
                                     rateUnit,
                                     durationUnit,
                                     filter);
        }
    }

    private final String topic;
    private final Producer<?,?> producer;
    private final KafkaMetricsSerializer serializer;

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReporter.class);

    private static final Callback errorCallback = new Callback() {
            public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e != null) {
                    LOGGER.error("Error sending metrics to Kafka", e);
                }
            }
        };

    public final TimeUnit metricRateUnit;
    public final TimeUnit metricDurationUnit;

    private KafkaReporter(MetricRegistry registry,
                          Producer<?,?> producer,
                          String topic,
                          KafkaMetricsSerializer serializer,
                          TimeUnit rateUnit,
                          TimeUnit durationUnit,
                          MetricFilter filter) {
        super(registry, "kafka-reporter", filter, rateUnit, durationUnit);
        this.producer = producer;
        this.topic = topic;
        this.serializer = serializer;
        this.metricRateUnit = rateUnit;
        this.metricDurationUnit = durationUnit;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final Date timestamp = java.util.Calendar.getInstance().getTime();
        final List<ProducerRecord<?,?>> messages = serializer.serialize(gauges,
                                                                        counters,
                                                                        histograms,
                                                                        meters,
                                                                        timers,
                                                                        topic,
                                                                        timestamp,
                                                                        metricRateUnit,
                                                                        metricDurationUnit);

        for (ProducerRecord message : messages) {
            producer.send(message, errorCallback);
        }
    }
}
