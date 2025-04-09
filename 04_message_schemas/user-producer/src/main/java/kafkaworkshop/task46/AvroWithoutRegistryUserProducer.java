package kafkaworkshop.task46;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Properties;
import kafkaworkshop.Color;
import kafkaworkshop.User;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class AvroWithoutRegistryUserProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ACKS_CONFIG, "all");
        props.setProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, AvroWithoutRegistryUserSerializer.class.getName());

        User user = User.newBuilder()
                .setName("andre")
                .setAge(101)
                .setFavoriteColor(Color.GREEN)
                .build();

        Producer<String, User> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("users-avro-without-registry", user.getName().toString(), user));
        producer.close();
    }
}

