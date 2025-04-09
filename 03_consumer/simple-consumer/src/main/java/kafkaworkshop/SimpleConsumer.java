package kafkaworkshop;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleConsumer {
    private static final Logger log = LoggerFactory.getLogger(SimpleConsumer.class);

    public static void main(String[] args) {
        solution_3_1();
        // solution_3_2(args);
    }

    private static void solution_3_1() {
        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(GROUP_ID_CONFIG, "my-first-consumer");
        props.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("numbers"));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("partition={}, offset = {}, key = {}, value = {}", record.partition(), record.offset(), record.key(), record.value());
                }
            }
        }
    }

    /**
     * start application using 3 parameters:
     * 1. name of the consumerGroup
     * 2. name of the consumer
     * 3. pause after each successful poll
     */
    private static void solution_3_2(String[] args) throws Exception {
        String consumerGroup = args[0];
        String consumerName = args[1];
        int sleepInterval = Integer.parseInt(args[2]);

        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(GROUP_ID_CONFIG, consumerGroup);
        props.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(AUTO_COMMIT_INTERVAL_MS_CONFIG, "50");
        props.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("numbers"));

        int count = 0;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                if (++count % 1000 == 0) {
                    log.info("{} has processed {} entries", consumerName, count);
                }

            }

            TimeUnit.MILLISECONDS.sleep(sleepInterval);
        }
    }
}
