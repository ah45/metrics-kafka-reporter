package io.dropwizard.metrics.kafka.serialization;

import java.io.StringWriter;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Metric;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.apache.kafka.clients.producer.ProducerRecord;

import com.codahale.metrics.json.MetricsModule;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Converts {@link Metric}s into UTF8 string encoded {@link
 * ProducerRecord}s where the key is the metric name and the value is
 * a JSON document comprising:
 *
 * <pre><code>
 * {
 *   ... the metrics properties ...,
 *   "timestamp": "2015-10-22T11:50:34.762Z"
 * }
 * </code></pre>
 *
 * <p>
 * The timestamp is given as an ISO 8601 serialized string. The metric
 * properties will vary by the type of metric being recorded. Using
 * Metrics version {@code 3.0.1} some example encodings are:
 * </p>
 *
 * <pre><code>
 * // Gauge
 * {"value": 3, "timestamp": "2015-10-22T11:50:34.762Z"}
 *
 * // Counter
 * {"count": 7, "timestamp": "2015-10-22T11:50:34.762Z"}
 *
 * // Meter
 * {
 *   "count": 7,
 *   "m1_rate": 2,
 *   "m5_rate": 4,
 *   "m15_rate": 3,
 *   "mean_rate": 1,
 *   "units": "events/second",
 *   "timestamp": "2015-10-22T11:50:34.762Z"
 * }
 *
 * // Histogram
 * {
 *   "count": 7,
 *   "max": 2,
 *   "mean": 4,
 *   "min": 3,
 *   "p50": 1,
 *   ... p75, p95, p98, p99 ...
 *   "p999": 1,
 *   "stddev": "0.013",
 *   "timestamp": "2015-10-22T11:50:34.762Z"
 * }
 * </code></pre>
 *
 * <p>
 * The exact encoding may change depending on the version of the
 * Metrics library in use. See {@link
 * com.codahale.metrics.json.MetricsModule} for exact details.
 * </p>
 *
 * <p>
 * Produces one message ({@link ProducerRecord}) per metric.
 * </p>
 *
 * <p>
 * Note that this is a singleton class: use the {@link #INSTANCE}
 * member rather than attempting to instantiate a new instance.
 * </p>
 */
public class JSONStringSerializer implements KafkaMetricsSerializer {
    /**
     * The singleton instance of this class.
     */
    public final static JSONStringSerializer INSTANCE = new JSONStringSerializer();

    protected JSONStringSerializer() {
    }

    private static class IsoFormatter {
        private final static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
        private final static TimeZone utc = TimeZone.getTimeZone("UTC");
        private static ThreadLocal<SimpleDateFormat> isoFormatter = new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat get() {
                    return super.get();
                }

                @Override
                protected SimpleDateFormat initialValue() {
                    SimpleDateFormat df = new SimpleDateFormat(ISO_FORMAT);
                    df.setTimeZone(utc);

                    return df;
                }

                @Override
                public void remove() {
                    super.remove();
                }

                @Override
                public void set(SimpleDateFormat df) {
                    super.set(df);
                }
            };

        public static String format(Date date) {
            return isoFormatter.get().format(date);
        }
    }

    /**
     * Performs the actual encoding of the JSON data into a {@link
     * ProducerRecord}.
     *
     * <p>
     * Sub classes can override this method to produce messages in
     * different formats without concerning themselves with the
     * conversion of the metrics into JSON documents. (As {@link
     * JSONByteArraySerializer} does.)
     * </p>
     *
     * @param topic the topic to which the message will be posted
     * @param key the key of the message
     * @param value the JSON formatted metric value
     * @return a Kafka {@link ProducerRecord}
     */
    protected ProducerRecord<?,?> metricRecord(String topic, String key, String value) {
        return new ProducerRecord<String, String>(topic, key, value);
    }

    /**
     * Given a generic collection of metrics of varying types,
     * converts them to JSON including an ISO 8601 formatted
     * timestamp, creates corresponding {@link ProducerRecord}s for
     * sending to Kafka, and adds them to the given message list.
     *
     * @param messageList the list to which the generated Kafka {@link
     * ProducerRecord}s should be added.
     * @param metrics the metrics to convert for publication to Kafka
     * @param topic the Kafka topic to which the metrics should be
     * published
     * @param timestamp the timestamp when the metrics were generated
     * @param jsonGenerator the {@link JsonGenerator} to use
     * @param buffer the buffer to use when constructing the JSON
     * message bodies
     *
     * @throws java.io.IOException if writing a metric to JSON fails
     */
    protected <T extends Metric> void addMetricMessages(List<ProducerRecord<?,?>> messageList,
                                                        SortedMap<String, T> metrics,
                                                        String topic,
                                                        Date timestamp,
                                                        JsonGenerator jsonGenerator,
                                                        StringWriter buffer)
        throws java.io.IOException
    {
        for (Map.Entry<String, T> metric : metrics.entrySet()) {
            // write the metric details
            jsonGenerator.writeObject(metric.getValue());

            // start a "new" object so as not to confuse the generator
            jsonGenerator.writeStartObject();
            // rewind over the "},{" sequence then add a "," to
            // "merge" the two objects
            jsonGenerator.flush();
            buffer.getBuffer().setLength(buffer.getBuffer().length() - 3);
            buffer.append(',');

            // add additional fields and close/flush the JSON
            jsonGenerator.writeObjectField("timestamp", IsoFormatter.format(timestamp));
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();

            messageList.add(metricRecord(topic, metric.getKey(), buffer.toString()));
            buffer.getBuffer().setLength(0);
        }
    }

    public List<ProducerRecord<?,?>> serialize(SortedMap<String, Gauge> gauges,
                                               SortedMap<String, Counter> counters,
                                               SortedMap<String, Histogram> histograms,
                                               SortedMap<String, Meter> meters,
                                               SortedMap<String, Timer> timers,
                                               String topic,
                                               Date timestamp,
                                               TimeUnit rateUnit,
                                               TimeUnit durationUnit)
        throws RuntimeException
    {
        try {
            final StringWriter messageBody = new StringWriter();
            final ObjectMapper mapper = new ObjectMapper()
                .disable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new MetricsModule(rateUnit, durationUnit, false));
            final JsonGenerator generator = mapper
                .getFactory()
                .createGenerator(messageBody);

            List<ProducerRecord<?,?>> messages = new ArrayList<ProducerRecord<?,?>>();

            addMetricMessages(messages, gauges, topic, timestamp, generator, messageBody);
            addMetricMessages(messages, counters, topic, timestamp, generator, messageBody);
            addMetricMessages(messages, histograms, topic, timestamp, generator, messageBody);
            addMetricMessages(messages, meters, topic, timestamp, generator, messageBody);
            addMetricMessages(messages, timers, topic, timestamp, generator, messageBody);

            return messages;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
