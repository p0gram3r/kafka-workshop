package kafkaworkshop;

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueJoiner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MovieRatingApp {
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String movieTopic = "movies";
    private static final String rekeyedMovieTopic = "movies-rekeyed";
    private static final String ratingTopic = "ratings";
    private static final String ratedMoviesTopic = "rated-movies";

    public static void main(String[] args) {
        final Properties streamProps = buildStreamsProperties();
        final Topology topology = buildTopology();

        final KafkaStreams streams = new KafkaStreams(topology, streamProps);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }

    private static Properties buildStreamsProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "movieRatingApp");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        return props;
    }

    public static Topology buildTopology() {
        final MovieRatingJoiner joiner = new MovieRatingJoiner();
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Movie> movieStream = builder.<String, Movie>stream(movieTopic)
                .map((key, movie) -> new KeyValue<>(String.valueOf(movie.getId()), movie));
        movieStream.to(rekeyedMovieTopic);

        KTable<String, Movie> movies = builder.table(rekeyedMovieTopic);

        KStream<String, Rating> ratings = builder.<String, Rating>stream(ratingTopic)
                .map((key, rating) -> new KeyValue<>(String.valueOf(rating.getId()), rating));

        KStream<String, RatedMovie> ratedMovie = ratings.join(movies, joiner);
        ratedMovie.to(ratedMoviesTopic, Produced.with(Serdes.String(), ratedMovieAvroSerde()));

        return builder.build();
    }

    private static SpecificAvroSerde<RatedMovie> ratedMovieAvroSerde() {
        final Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);

        final SpecificAvroSerde<RatedMovie> movieAvroSerde = new SpecificAvroSerde<>();
        movieAvroSerde.configure(serdeConfig, false);

        return movieAvroSerde;
    }
}

class MovieRatingJoiner implements ValueJoiner<Rating, Movie, RatedMovie> {
    public RatedMovie apply(Rating rating, Movie movie) {
        return RatedMovie.newBuilder()
                .setId(movie.getId())
                .setTitle(movie.getTitle())
                .setReleaseYear(movie.getReleaseYear())
                .setRating(rating.getRating())
                .build();
    }
}