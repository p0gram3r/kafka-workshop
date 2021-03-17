package kafkaworkshop;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;
import java.util.stream.Stream;

public class ArticleFilterApp {
    public static void main(final String[] args) {
        Topology topology = buildTopology("streams-text-input", "streams-lines-with-article");
        Properties props = buildStreamProperties();

        KafkaStreams streams = new KafkaStreams(topology, props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }

    private static Properties buildStreamProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "articlefilter-application");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
    }

    private static Topology buildTopology(String inputTopic, String outputTopic) {
        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()))
                .filter((key, value) -> containsArticle(value))
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }

    private static boolean containsArticle(String value) {
        return Stream.of(value.split("\\W"))
                .map(String::toLowerCase)
                .anyMatch(w -> w.equals("a") || w.equals("an") || w.equals("the"));
    }
}