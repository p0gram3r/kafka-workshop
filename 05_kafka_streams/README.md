# 5 - Introduction to Kafka Streams
Kafka Streams is a client library for building mission-critical real-time
applications and microservices, where the input and/or output data is stored
in Kafka clusters. Kafka Streams combines the simplicity of writing and
deploying standard Java applications on the client side with the benefits of
Kafka's server-side cluster technology to make these applications highly
scalable, elastic, fault-tolerant, distributed, and much more.

To learn more about the Kafka Streams API, please refer to the following documents:
- [Architecture](https://kafka.apache.org/25/documentation/streams/architecture)
- [Streams DSL](https://kafka.apache.org/25/documentation/streams/developer-guide/dsl-api.html)


## Level 5.1 - Simple Filter
A very common use case when processing a topic is filtering messages based on specific criteria.

Let's create a very basic Kafka Streams application named `ArticleFilterApp` that searches the content of the
`streams-text-input` topic for articles and forwards matching lines to another topic named 
`streams-lines-with-article`.

**Tasks**
1. Create two topics `streams-text-input` and `streams-lines-with-article`.
2. Add the Kafka Streams dependency to the Maven POM.
3. Time to write the actual application! First, create a method to build a
   [topology](https://kafka.apache.org/25/javadoc/index.html?org/apache/kafka/streams/Topology.html).
    ```
    private static Topology buildTopology(String inputTopic, String outputTopic) {
       StreamsBuilder builder = new StreamsBuilder();
       builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()))
                .filter((key, value) -> containsArticle(value))
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));
    
       return builder.build();
    }
    ```
4. Next, implement the filter method that matches all lines containing an article ("a", "an", "the").
5. Build up the configuration properties of the app:
    ```
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "article-filter-app");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    ```
6. Finally, create an instance of `KafkaStreams` using the topology and configuration properties. To allow your
   application to gracefully shutdown in response to SIGTERM, it is recommended to add a shutdown hook and close
   the stream.
    ```
    KafkaStreams streams = new KafkaStreams(topology, props);
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    streams.start();
    ```
7. Start a console consumer to read the content of `streams-lines-with-article`
8. Start the application. Use a console producer to send some text messages. Check the content of the target topic.

**Learning Objectives**
- understanding the difference between regular consumers and clients using Kafka Streams
- performing stateless operation on streams


## Level 5.2 - Materializing Data in Stateful Applications
In this exercise we will implement a WordCountApp, which computes a
word occurrence histogram from a given text. However, this application is
designed to operate on an **infinite, unbounded stream** of data! It is a
stateful algorithm that tracks and updates the counts of words. It will
periodically output its current state and results while continuing to process
more data because it cannot know when it has processed "all" the input data.

**Tasks**
1. Create a topic named `streams-word-count`. The results of the count operation will be stored in this topic.
   Since the count is continually updated per word, the cleanup policy should be set to "compact".
2. Write the application. Use the following topology:
   ```
   private static Topology buildTopology(String inputTopic, String outputTopic)
      StreamsBuilder builder = new StreamsBuilder();

      KStream<String, String> textLines = builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()));
      KTable<String, Long> wordCounts = textLines
              .flatMapValues(line -> Arrays.asList(line.toLowerCase().split("\\W+")))
              .groupBy((key, word) -> word)
              .count(Materialized.as("word-count-store"));
      wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long())
      );

      return builder.build();
   }
   ```
3. Start a console consumer to read from the target topic. Use 
   `--property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer` to see the actual numbers.
4. Start the applications, alongside the ArticleFilterApp. Start another console consumer to read the output of
   the ArticleFilterApp.
5. Use a console producer to send some text messages to the `streams-text-input` topic.
6. Use the `kafka-topics` command to inspect the topics automatically created by the WordCountApp.

**Bonus**
- Write a producer that regularly (e.g. every other second) sends a few lines of text to make sure the stream of
  text lines never ends.

**Learning Objective**
- - performing stateful operation on streams


## Level 5.3 - Writing a Custom Serde
To be able to write a Kafka Streams application for a topic containing a custom text or binary format (e.g. JSON
or Avro), we have to provide a custom
[Serde](https://kafka.apache.org/25/javadoc/index.html?org/apache/kafka/common/serialization/Serde.html).
Serdes are simple factories for creating serializers and deserializers for a specific format.

**Tasks**
1. Create a `SeniorUsersFilterApp` that sends all `users` (see level 4.3) of age 60+ to a new topic `senior-users`. Use
   the `SpecificAvroSerde` to parse the entities from the source topic.

**Learning Objectives**
- using custom Serde factories
- working with message schemas and schema registries within a Streams application


## Level 5.4 - Joining data of two topics
Suppose we have a set of movies that have been released, and a stream of ratings about how entertaining they are. In
this exercise, we'll write a program that joins each rating with content about the movie.

1. Add the `io.confluent:kafka-streams-avro-serde` dependency to the Maven POM.
2. This application will make use of three topics:
   - one called `movies` that holds movie reference data:
    ```
   {
      "namespace": "kafkaworkshop",
      "type": "record",
      "name": "Movie",
      "fields": [
        {"name": "id", "type": "long"},
        {"name": "title", "type": "string"},
        {"name": "release_year", "type": "int"}
      ]
    }
    ```
   - one called `ratings` that holds a stream of inbound movie ratings
    ```
    {
      "namespace": "kafkaworkshop",
      "type": "record",
      "name": "MovieRating",
      "fields": [
        {"name": "id", "type": "long"},
        {"name": "rating", "type": "double"}
      ]
    }
    ```
   - one called `rated-movies` that holds the result of the join between ratings and movies
    ```
    {
      "namespace": "kafkaworkshop",
      "type": "record",
      "name": "RatedMovie",
      "fields": [
        {"name": "id", "type": "long"},
        {"name": "title", "type": "string"},
        {"name": "release_year", "type": "int"},
        {"name": "rating", "type": "double"}
      ]
    }
    ```

   Create the respective topics and Avro schema files and generate the code of the POJOs.
3. Build the topology for this app.
   ```
   public static Topology buildTopology(String movieTopic, String ratingTopic, String ratedMoviesTopic) {
       final StreamsBuilder builder = new StreamsBuilder();
       final MovieRatingJoiner joiner = new MovieRatingJoiner();

       final KStream<String, Movie> movieStream = builder.<String, Movie>stream(movieTopic)
               .map((key, movie) -> new KeyValue<>(movie.getId().toString(), movie));
       movieStream.to(rekeyedMovieTopic);

       final KTable<String, Movie> movies = builder.table(rekeyedMovieTopic);
       final KStream<String, Rating> ratings = builder.<String, Rating>stream(ratingTopic)
               .map((key, rating) -> new KeyValue<>(rating.getId().toString(), rating));
       final KStream<String, RatedMovie> ratedMovie = ratings.join(movies, joiner);
       ratedMovie.to(ratedMoviesTopic, Produced.with(Serdes.String(), ratedMovieAvroSerde()));

       return builder.build();
   }

   private static SpecificAvroSerde<RatedMovie> ratedMovieAvroSerde() {
       final Map<String, String> serdeConfig = new HashMap<>();
       serdeConfig.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,SCHEMA_REGISTRY_URL);

       final SpecificAvroSerde<RatedMovie> movieAvroSerde = new SpecificAvroSerde<>();
       movieAvroSerde.configure(serdeConfig, false);

       return movieAvroSerde;
   }
   ```

   The first thing the topology does is streaming the movies. The problem is
   that we shouldn’t make any assumptions about the key of this stream, so we
   have to repartition it explicitly. We use the `map()` method for that,
   creating a new KeyValue instance for each record, using the movie ID as the
   new key.

   The movies start their life in a stream, but fundamentally, movies are
   entities that belong in a table. To turn them into a table, we first emit
   the rekeyed stream to a Kafka topic using the `to()` method. We can then
   use the `builder.table()` method to create a `KTable<String,Movie>`. We
   have successfully turned a topic full of movie entities into a scalable,
   key-addressable table of Movie objects. With that, we’re ready to move on
   to ratings.

   Creating the `KStream<String,Rating>` of ratings looks just like our first
   step with the movies: we create a stream from the topic, then repartition
   it with the `map()` method. Note that we must choose the same key (movie
   ID) for our join to work.

   With the ratings stream and the movie table in hand, all that remains is to
   join them using the join() method. It’s a simple one-liner, but we have
   concealed a bit of complexity in the form of the MovieRatingJoiner class.
3.  Implement the MovieRatingJoiner class.
    ```
    class MovieRatingJoiner implements ValueJoiner<Rating, Movie, RatedMovie> {
        @Override
        public RatedMovie apply(Rating rating, Movie movie) {
            // FIXME
            return null;
        }
    }
    ```
    When joining two tables in a relational database, by default we get a new
    table containing all the columns of the left table plus all the columns of
    the right table. When joining a stream and a table in Kafka, we get a new
    stream, but we must be explicit about the value of that stream - the
    combination between the value in the stream, and the associated value in the
    table. The ValueJoiner interface in the Streams API does this work. The
    single apply() method takes the stream and table values as parameters, and
    returns the value of the joined stream as output. This is just a matter of
    creating a `RatedMovie` object and populating it with the relevant fields of
    the input movie and rating.

    This can be done in a Java Lambda in the call to the `join()` method where
    we are building the stream topology, but the joining logic may become
    complex, and breaking it off into its own testable class is a good idea.
4. Add all missing pieces to the app and start it.
5. Load in some movie reference data to your `movies` topic:
   ```
   {"id": 294, "title": "Die Hard", "release_year": 1988}
   {"id": 354, "title": "Tree of Life", "release_year": 2011}
   {"id": 782, "title": "A Walk in the Clouds", "release_year": 1995}
   {"id": 128, "title": "The Big Lebowski", "release_year": 1998}
   {"id": 780, "title": "Super Mario Bros.", "release_year": 1993}
   ```
6. Start consuming your `rated-movies` topic. This way, as soon as ratings are
   joined to movies, we’ll see the results right away.
7. Produce some movie ratings and check the consumer:
   ```
   {"id": 294, "rating": 8.2}
   {"id": 294, "rating": 8.5}
   {"id": 354, "rating": 9.9}
   {"id": 354, "rating": 9.7}
   {"id": 782, "rating": 7.8}
   {"id": 782, "rating": 7.7}
   {"id": 128, "rating": 8.7}
   {"id": 128, "rating": 8.4}
   {"id": 780, "rating": 2.1}
   ```

**Learning Objectives**
- joining data of two topics to create new content 
