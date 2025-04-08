# 5 - Introduction to Kafka Streams

Kafka Streams is a client library for building mission-critical real-time
applications and microservices, where the input and/or output data is stored
in Kafka clusters. Kafka Streams combines the simplicity of writing and
deploying standard Java applications on the client side with the benefits of
Kafka's server-side cluster technology to make these applications highly
scalable, elastic, fault-tolerant, distributed, and much more.

To learn more about the Kafka Streams API, please refer to the following documents:

- [Architecture](https://kafka.apache.org/40/documentation/streams/architecture)
- [Streams DSL](https://kafka.apache.org/40/documentation/streams/developer-guide/dsl-api.html)


## Level 5.1 - Simple Filter
A very common use case when processing a topic is filtering messages based on specific criteria.

Let's create a very basic Kafka Streams application named `ArticleFilterApp` that searches the content of the
`streams-text-input` topic for articles and forwards matching lines to another topic named 
`streams-lines-with-article`.

### Learning Objectives
- understanding the difference between regular consumers and clients using Kafka Streams
- performing stateless operation on streams

### Tasks
1. Create two topics `streams-text-input` and `streams-lines-with-article`.
2. Add the Kafka Streams dependency to the Maven POM.
3. Time to write the actual application! First, create a method to build a
   [topology](https://kafka.apache.org/25/javadoc/index.html?org/apache/kafka/streams/Topology.html).
    ```java
    private static Topology buildTopology(String inputTopic, String outputTopic) {
       StreamsBuilder builder = new StreamsBuilder();
       builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()))
                .filter((key, value) -> containsArticle(value))
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));
    
       return builder.build();
    }
    ```
4. Next, implement the filter method that matches all lines containing an article ("a", "an", "the").
5. Build up the configuration properties of the app.
6. Finally, create an instance of `KafkaStreams` using the topology and configuration properties. To allow your
   application to gracefully shutdown in response to SIGTERM, it is recommended to add a
   shutdown hook to close the stream.
    ```java
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    ```
7. Start a console consumer to read the content of `streams-lines-with-article`
8. Start the application. Use a console producer to send some text messages. Check the content of the target topic.


## Level 5.2 - Materializing Data in Stateful Applications

In this exercise we will implement a WordCountApp, which computes a
word occurrence histogram from a given text. However, this application is
designed to operate on an **infinite, unbounded stream** of data! It is a
stateful algorithm that tracks and updates the counts of words. It will
periodically output its current state and results while continuing to process
more data because it cannot know when it has processed "all" the input data.

### Learning Objective
- performing stateful operation on streams

### Tasks

1. Create a topic named `streams-word-count`. The results of the count operation will be stored in this topic.
   Since the count is continually updated per word, the cleanup policy should be set to "compact".
2. Write the application. Use the following topology:
   ```java
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

### Bonus
- Write a producer that regularly (e.g. every other second) sends a few lines of text to make sure the stream of
  text lines never ends.


## Level 5.3 - Writing a Custom Serde

To be able to write a Kafka Streams application for a topic containing a custom text or
binary format (e.g. JSON or Avro), we have to provide a custom
[Serde](https://kafka.apache.org/25/javadoc/index.html?org/apache/kafka/common/serialization/Serde.html).
Serdes are simple factories for creating serializers and deserializers for a specific format.

### Learning Objectives
- using custom Serde factories
- working with message schemas and schema registries within a Streams application

### Tasks
1. Create a `SeniorUsersFilterApp` that sends all `users` (see level 4.3) of age 60+ to a new topic `senior-users`. Use the `SpecificAvroSerde` to parse the entities from the source topic.


## Level 5.4 - Joining data of two topics

Suppose we have a set of movies that have been released, and a stream of ratings about how entertaining they are. In
this exercise, we'll write a program that joins each rating with content about the movie.

### Learning Objectives
- joining data of two topics to create new content

### Tasks

1. Add the `io.confluent:kafka-streams-avro-serde` dependency to the Maven POM.
2. This application will make use of three topics:
   - one called `movies` that holds movie reference data:
    ```json
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
    ```json
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
    ```json
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
   ```java
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
4. Implement the MovieRatingJoiner class.
   ```java
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
5. Add all missing pieces to the app and start it.
6. Load in some movie reference data to your `movies` topic:
   ```json
   {"id": 294, "title": "Die Hard", "release_year": 1988}
   {"id": 354, "title": "Tree of Life", "release_year": 2011}
   {"id": 782, "title": "A Walk in the Clouds", "release_year": 1995}
   {"id": 128, "title": "The Big Lebowski", "release_year": 1998}
   {"id": 780, "title": "Super Mario Bros.", "release_year": 1993}
   ```
7. Start consuming your `rated-movies` topic. This way, as soon as ratings are
   joined to movies, we’ll see the results right away.
8. Produce some movie ratings and check the consumer:
   ```json
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


## Level 5.5 - Putting it all together

We are the development team of a FinTech company which processes millions of transactions every day.
To prevent fraud, money laundery, and high-risk activity, a real-time transaction monitoring system
is being developed. It helps detecting suspicious transactions and performing real-time risk scoring.

The following topics contain the relevant information we need to process:

- `transactions` = Incoming payment data
   ```json
   {
      "transactionId": "tx123",
      "userId": "user42",
      "amount": 8400.50,
      "currency": "EUR",
      "timestamp": "2025-04-08T14:52:00Z",
      "country": "DE"
   }
   ```
- `user-profiles` - static user information
   ```json
   {
      "userId": "user42",
      "accountAgeDays": 120,
      "isBusinessAccount": false,
      "homeCountry": "DE"
   }
   ```
- `login-events` - recent login events
   ```json
   {
      "userId": "user42",
      "deviceId": "dev-abc",
      "ip": "85.123.99.22",
      "country": "NL",
      "timestamp": "2025-04-08T14:50:00Z"
   }
   ```
- `blacklisted-countries` - dynamically updated list of high-risk countries (e.g., "NG", "RU", "IR")
   ```json
   {
      "NG": "true"
   }
   ```
- `known-devices` - devices the user has previously logged in from
   ```json
   {
      "deviceId": "device-xyz",
      "knownSince": "2024-12-01"
   }
   ```


### Learning Objectives
- joining data of multiple topics, applying time-windowed logic, aggregations, and enrichment with external data

### Tasks

1. Enrich `transactions` with data from `user-profiles`, `blacklisted-countries`, and the latest login event for the user (within a 5-minute time window).

2. Implement transformation to assign a riskLevel to each transaction based on these rules:
   - If the transaction originates from a blacklisted country → High Risk
   - If the transaction comes from a new device or country → Medium Risk
   - If the amount > 10,000 and account age < 30 days → High Risk
   - Otherwise → Low Risk
   - **Bonus:** Instead of categorical risk, output a numeric riskScore from 1–10.

3. Count all High-Risk transactions per user in the last 15 minutes using a tumbling window, and write results to a `user-risk-aggregates` topic.

4. Write consumer applications to output the following topics:
   - `scored-transactions` - contains enriched transactions + riskLevel + reason
   - `user-risk-aggregates` - contains userId, window, and high-risk transaction count

### Bonus

- Implement a REST endpoint to retrieve a user’s current risk stats from Kafka Streams' state store.
