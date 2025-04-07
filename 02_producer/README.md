# 2 - Writing Messages to Kafka

## Level 2.1 - Introduction to Producers

### Learning Objectives

- basic understanding of how to write a producer application
- understanding the message order semantics within a topic

### Tasks

Write a small application that send some messages to a Kafka topic.

1. Create a topic `numbers` with at least 3 partitions.
2. Update the `SimpleProducerApp` and let it write 100 numbers to the topic.
3. Use a console consumer to consume these numbers. Pay special attention to the order of the output.

### Bonus

- The app can be run as a standalone executable and can be configured via env variables. The following parameters can
  be passed:
  - bootstrap servers
  - name of the topic to send to
  - number of messages to send
- Create a Kubernetes `Job` using that application.

## Level 2.2 - Throughput vs. durability

In Kafka, many trade-offs can be made in terms of performance and message durability. Some of these aspects pertain
the producers of messages.

Let's examine the effects of various producer config properties and the impact of synchronous and asynchronous sends.

### Learning Objectives

- understanding the tradeoff between reliability and producer latency
- understanding the implications of actively waiting for a broker's response

### Tasks

1. Add a stopwatch (e.g. `org.apache.commons:commons-lang3`) to the producer app which measures the duration of the
   send process.
2. Measure the time it takes to send one million messages **asynchronously** using different producer configs:
    - `acks`: 0, 1, all
    - `batch.size`: 20, 200, 2000, 20000
3. Repeat the experiment using a **sync** producer instead of an async one. **How to explain the difference?**

## Level 2.3 - Partitioning behavior

By default, Kafka assigns a message to a partition by using a dedicated function. In this exercise, we want to assign
the partitions manually.

### Learning Objectives

- knowing ways to influence the producer partitioning strategy and being aware of possible side effects

### Tasks

1. Create a topic named `manual-partitioning` with 10 partitions.
2. Write a producer that generates 1,000 random integers and sends them to this topic. The lowest digit of each number
   determines the number of the partition to send the message to.
3. Use a console consumer to check the content of each partition.
4. Modify the topic by increasing the number of partitions to 15.
5. Send another 1,000 random integers and check the content of the new partitions.

### Bonus

- Implement a custom `org.apache.kafka.clients.producer.Partitioner` instead of assigning the partitions manually.
  Enable it by setting property `partitioner.class` in the producer config.
