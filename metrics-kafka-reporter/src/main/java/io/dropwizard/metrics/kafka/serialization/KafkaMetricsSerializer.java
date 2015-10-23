package io.dropwizard.metrics.kafka.serialization;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * An interface to convert {@link com.codahale.metrics.Metric Metric}s
 * to Kafka messages (as key/value encompassing {@link
 * ProducerRecord}s.)
 */
public interface KafkaMetricsSerializer {
    /**
     * Given maps of the metrics to report (one per type: gauges,
     * counters, histograms, meters, and timers) returns a list of
     * corresponding messages that should be posted to Apache Kafka.
     *
     * <p>
     * Each message in the returned list should be a {@link
     * ProducerRecord} populated with an appropriate topic, partition,
     * key, and value for posting to Kafka. The default topic of the
     * calling {@link io.dropwizard.metrics.kafka.KafkaReporter
     * KafkaReporter} is provided as an argument.
     * </p>
     *
     * <p>
     * The serializer is free to return as many or as few messages as
     * it wants: one message per metric, one message containing all
     * the metrics, or anything in between.
     * </p>
     *
     * @param gauges a map of the {@link Gauge} metrics to publish
     * @param counters a map of the {@link Counter} metrics to publish
     * @param histograms a map of the {@link Histogram} metrics to publish
     * @param meters a map of the {@link Meter} metrics to publish
     * @param timers a map of the {@link Timer} metrics to publish
     * @param topic the default topic to publish the metrics to
     * @param timestamp the timestamp when the metrics were generated
     * @param rateUnit the rate unit used for sampling the metrics
     * @param durationUnit the duration unit used for sampling the metrics
     * @return a {@link List} of {@link ProducerRecord}s to publish to Kafka
     */
    public List<ProducerRecord<?,?>> serialize(SortedMap<String, Gauge> gauges,
                                               SortedMap<String, Counter> counters,
                                               SortedMap<String, Histogram> histograms,
                                               SortedMap<String, Meter> meters,
                                               SortedMap<String, Timer> timers,
                                               String topic,
                                               Date timestamp,
                                               TimeUnit rateUnit,
                                               TimeUnit durationUnit)
        throws RuntimeException;
}
