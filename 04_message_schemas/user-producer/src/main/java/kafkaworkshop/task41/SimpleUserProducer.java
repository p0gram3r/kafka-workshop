package kafkaworkshop.task41;

import static kafkaworkshop.task41.SimpleUser.Color.blue;
import static kafkaworkshop.task41.SimpleUser.Color.green;
import static kafkaworkshop.task41.SimpleUser.Color.yellow;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SimpleUserProducer {
    private static final Logger log = LoggerFactory.getLogger(SimpleUserProducer.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", SimpleUserSerializer.class.getName());

        SimpleUser user1 = SimpleUser.builder().name("Andre").age(41).favoriteColor(green).build();
        SimpleUser user2 = SimpleUser.builder().name("Homer").age(42).favoriteColor(yellow).build();
        SimpleUser user3 = SimpleUser.builder().name("Marge").age(40).build();

        Producer<String, SimpleUser> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("simple-users", user1.getName(), user1));
        producer.send(new ProducerRecord<>("simple-users", user2.getName(), user2));
        producer.send(new ProducerRecord<>("simple-users", user3.getName(), user3));

        log.info("updating {}", user3.getName());
        user3.setFavoriteColor(blue);
        producer.send(new ProducerRecord<>("simple-users", user3.getName(), user3));

        log.info("deleting {}", user2.getName());
        producer.send(new ProducerRecord<>("simple-users", user2.getName(), null));

        log.info("... DONE!");
        producer.close();
    }
}

