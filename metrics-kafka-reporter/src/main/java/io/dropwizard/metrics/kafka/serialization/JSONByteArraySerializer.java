package io.dropwizard.metrics.kafka.serialization;

import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * As per {@link JSONStringSerializer} but returning byte encoded
 * {@link ProducerRecord}s. The mapping from metric to message is the
 * same as is the message content. These classes only differ in the
 * encoding used for the returned {@link ProducerRecord} instances.
 *
 * <p>
 * Use the one which matches your Kafka serializer.
 * </p>
 */
public class JSONByteArraySerializer extends JSONStringSerializer {
    public final static JSONByteArraySerializer INSTANCE = new JSONByteArraySerializer();

    private final static String encoding = "UTF8";

    protected ProducerRecord<?,?> metricRecord(String topic, String key, String value) {
        try {
            return new ProducerRecord<byte[], byte[]>(topic,
                                                      key.getBytes(encoding),
                                                      value.getBytes(encoding));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error serializing metric to byte[] as " + encoding, e);
        }
    }
}
