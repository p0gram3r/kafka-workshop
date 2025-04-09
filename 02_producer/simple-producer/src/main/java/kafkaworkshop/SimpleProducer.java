package kafkaworkshop;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Properties;
import java.util.Random;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProducer {
    private static final Logger log = LoggerFactory.getLogger(SimpleProducer.class);

    public static void main(String[] args) throws Exception {
        solution_2_1();
//        solution_2_2();
//        solution_2_3();
    }

    private static void solution_2_1() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ACKS_CONFIG, "all");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Producer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>("numbers", Integer.toString(i), Integer.toString(i)));
        }
        producer.close();
        log.info("level 2.1 ... DONE!");
    }


    private static void solution_2_2() throws Exception {
        int numbersToSend = 1_000_000;
        String[] acks = {"0", "1", "all"};
        int[] batchSizes = {20, 200, 2000, 20000};

        StopWatch sw = new StopWatch();

        for (String ack : acks) {
            for (int batchSize : batchSizes) {
                sw.reset();
                sw.start();

                Properties props = new Properties();
                props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                props.put(ACKS_CONFIG, ack);
                props.put(BATCH_SIZE_CONFIG, Integer.toString(batchSize));
                props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

                Producer<String, String> producer = new KafkaProducer<>(props);
                for (int i = 0; i < numbersToSend; i++) {
                    ProducerRecord<String, String> record = new ProducerRecord<>("numbers", Integer.toString(i), Integer.toString(i));

                    // async send
                    producer.send(record);

                    // sync send --> wait for response
                    // producer.send(record).get();
                }
                producer.close();

                sw.stop();
                log.info("### acks={} batchSize={} --- time: {}", ack, batchSize, sw);
            }
            log.info("level 2.2 ... DONE!");
        }
    }


    private static void solution_2_3() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Random random = new Random(System.currentTimeMillis());
        Producer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 0; i < 1000; i++) {
            int number = random.nextInt();
            int partition = Math.abs(number) % 10;
            String val = Integer.toString(number);
            producer.send(new ProducerRecord<>("manual-partitioning", partition, val, val));
        }

        log.info("level 2.3 ... DONE!");
        producer.close();
    }
}
