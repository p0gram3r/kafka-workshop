# 4 - Working with message schemas

One thing we'll have to do when working with Kafka is picking a data format for
our messages. It is very important to be consistent across your usage. Any
format, provided it is used consistently across the board, is better than a
mishmash of ad hoc choices.

In the previous exercises we have sent and consumed plain text messages
containing simple strings. Up next, we will deal with objects using JSON and a
popular binary format.

## Prerequisites

The following exercises make use of some Confluent specific components
(e.g. `KafkaAvroSerializer`) that spare us from writing
our own serializer and deserializer. For this to work, we need a
[Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
up and running. The registry provides us with the possibility to upload and
manage schemas for message keys and values. This will help keeping the data
of a topic consistent.

For convenience, a docker compose script is provided that spins up an environment with the following components

- a single ZooKeeper instance
- one Kafka broker - `localhost:9092`
- a Confluent schema registry - (http://localhost:8081)
- a UI for Confluent's schema registry - (http://localhost:8000)
- an AKHQ instance - (http://localhost:9000)

Use the following commands to manage the environment. Note, that this setup does not use any persistent volumes, so all data will be lost!

```bash
# start environment as daemon
docker-compose up -d

# shut everything down
docker-compose down
```

## Level 4.1 - Dealing with Objects

Instead of sending primitive integers or strings, producers can also deal with
objects. This requires the usage of a custom `Serializer` that is used to
convert an instance of a POJO into bytes.

For this exercise, let's create a `SimpleUser` class with the properties
`name`, `age` and `favoriteColor`. The name shall be mandatory, all other
fields are optional.

```java
class SimpleUser {
    public enum Color {
        red, yellow, blue, green
    }

    private String name;
    private int age;
    private Color favoriteColor;
}
```

For the sake of simplicity, the resulting JSON should look like the following:

```json
{
  "name": "Alice",
  "age": 33,
  "favorite_color": "green"
}
```

### Learning Objectives

- understanding of how to send complex data in events

### Tasks

1. Implement a custom `Serializer` that converts users into a JSON format.
2. Create a producer app to send "create", "update" and "delete" events. Inspect the records in the Kafka topic using AKHQ.
3. Describe ways to store user data more efficiently.

## Level 4.2 - The Dangers of Schemaless Messages

A very common issue when implementing stream processing pipelines with Kafka
is a mismatch between the expected and actual message format. Kafka itself
does not care about the message content, so it does not have any build-in
mechanisms for ensuring data consistency. Let's examine this problem.

### Learning Objectives

- understanding the risks of incompatible data format

### Tasks

1. Create an application that it can consume `SimpleUser` records.
2. Let the producer send a continuous flow of messages. Use the new consumer app to parse these messages.
3. Use another producer (e.g.: `kafka-console-producer`) to send a few messages that do not comply with the known user schema, e.g. by introducing additional fields or sending entirely different content. Examine the behaviour of the consumer application.
4. Describe different ways to deal with this situation.

## Level 4.3 - Avro Producer

Dealing with plain text message formats like XML and JSON allows us to quickly
inspect the content of a topic or build additional producers and consumers.
However, one downside of that approach is an increased message size. This might
not be an issue for small use cases with only a couple of thousand message to
deal with. On a big scale, where billions of messages have to be processed, and
potentially saved for a long period of time, this might not be the best
approach.

One alternative is to use a binary data format for messages. One popular binary
data format for Kafka messages is
[Apache Avro](http://avro.apache.org/docs/current/). It is an open source data
serialization system that helps with data exchange between systems. Avro helps
define a binary format for data, as well as map it to different programming
languages.

A good explanation of why using Avro for Kafka data can be found in the
[Confluent blog](https://www.confluent.io/blog/avro-kafka-data/).

### Learning Objectives

- sending events with binary message format
- using messages schemas to avoid incompatible message format

### Tasks

1. Place the following file at `src/main/avro/user.avsc`

   ```json
   {
      "namespace": "kafkaworkshop",
      "type": "record",
      "name": "User",
      "fields": [
         {"name": "name", "type": "string"},
         {"name": "age", "type": "int"},
         { 
            "name": "favorite_color",
            "type": {
               "name": "Color",
               "type": "enum",
               "symbols": ["RED", "YELLOW", "GREEN", "BLUE", "PINK", "PURPLE", "MAGENTA"]
            }
         }
      ]
   }
   ```

2. Add the `org.apache.avro:avro` dependency to the Maven POM. See Avro's
   [Getting Started Guide](https://avro.apache.org/docs/current/gettingstartedjava.html) for hints.
3. Add the `avro-maven-plugin` to the build plugins section of the Maven POM.
4. Turn the `.avsc` files into Java code.
5. Write the basic Kafka producer code. Use topic "users" as destination for the messages.
6. Add the `io.confluent:kafka-avro-serializer` dependency to the Maven POM. This requires an additional repository:
   `https://packages.confluent.io/maven`.
7. Change the value serializer class to `KafkaAvroSerializer`.
8. Add the property `KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG` and point it to the URL of the Confluent
   Schema Registry.
9. Create a couple of example users and send them to the topic.
10. Inspect the topic.
11. Use the REST API of the Confluent Schema Registry to inspect the schema stored in the registry.

## Level 4.4 - Avro Consumer

Now, let's create a consumer that is able to read users from Kafka.

### Learning Objectives

- consuming events with binary message format
- using messages schemas to prevent consuming messages with incompatible message format

## Tasks

1. Use the schema file to create the User class in the consumer project.
2. Write the basic Kafka consumer code. Change the deserializer class to `KafkaAvroDeserializer`.
3. Add the property `KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG` and point it to the URL of the Confluent
   Schema Registry.
4. Start the consumer and fetch the users written to the topic. If the consumer does not retrieve any data, find ways to convince it.
5. Inspect the data retrieved with a debugger. **What is different from what we expected? How to fix that?**

## Level 4.5 - Evolution of a Schema

An important aspect of data management is schema evolution. After the initial schema has been defined, applications may
need to evolve it over time. When this happens, it’s critical for the downstream consumers to be able to handle data
encoded with both the old and the new schema seamlessly.

When using Avro or other schema formats, one of the most important things is to manage the schemas and consider how
these schemas should evolve. The Confluent Schema Registry helps us with that by performing schema compatibility checks.
Check out the documentation at [Schema Evolution and Compatibility](https://docs.confluent.io/current/schema-registry/avro.html).

Let's add a backward compatible change to our schema.

### Learning Objectives

- evolving message format using schemas

### Tasks

1. Add a new optional field "last_name" and update producer and consumer **in the correct order.**
2. Add another backward compatible change to the schema and update only the producer. **What happens on the consumer side?**
3. Add an incompatible change to the schema and try producing and consuming a message. **What happens?**

## Level 4.6 - Using Avro without Schema Registry

Although highly recommended, we don't have to use a schema registry when using Avro for serializing and deserializing
messages.

Let's create a custom Avro serializer and serializer for our user that does not interact with a schema registry and
use them to send and consume users

### Learning Objectives

- using binary format without external schema registries

### Tasks

1. Create a separate project called `avro-without-registry`.
2. Add the avro schema file and all required components to generate the User class.
3. Create the custom serializer and deserializer. This requires the dependency `org.apache.kafka:kafka-clients`.
4. Build and upload the library to your local maven repository via `mvn install`.
5. Remove all generated user related code from the producer and consumer. Include the library and use its capabilities.
6. Send and consume some data to a different topic, e.g. `users2`

### Bonus

- Add a backward compatible change to the schema and update the consumers and producers. **What happens?**
- Add an incompatible change to the schema and update the consumers and producers. **What happens?**
