package kafkaworkshop.task46;

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
import kafkaworkshop.User;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroWithoutRegistryUserConsumer {
    private static final Logger log = LoggerFactory.getLogger(AvroWithoutRegistryUserConsumer.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(GROUP_ID_CONFIG, "my-group");
        props.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(AUTO_COMMIT_INTERVAL_MS_CONFIG, "36000000");
        props.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, AvroWithoutRegistryUserDeserializer.class.getName());

        try (KafkaConsumer<String, User> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("users-avro-without-registry"));
            while (true) {
                ConsumerRecords<String, User> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, User> record : records) {
                    log.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), record.value());

                    User user = record.value();
                    log.info(user.getName().toString());
                }
            }
        }
    }
}

