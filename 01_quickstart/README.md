# 1 - Kafka Basics

## Level 1.1 - My first Kafka cluster

1. Download Kafka

   ```bash
   wget https://dlcdn.apache.org/kafka/3.9.0/kafka_2.13-3.9.0.tgz
   tar -xzf kafka_2.13-3.9.0.tgz
   cd kafka_2.13-3.9.0
   ```

2. Start ZooKeeper

   ```bash
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```

3. Start a broker

   ```bash
   bin/kafka-server-start.sh config/server.properties
   ```

4. Create a topic

   ```bash
   bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --partitions 3 --topic quick-start
   ```

5. Get some information on the newly created topic

   ```bash
   bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
   bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic quick-start
   ```

   - Which broker is the leader of which partition?
   - Check the replica and ISR lists of the topic

6. Produce some messages

   Kafka comes with a command line client that reads input from a file or standard input and sends it to the
   Kafka cluster. By default, each line is sent as a separate message.

   ```bash
   bin/kafka-console-producer.sh --broker-list localhost:9092 --topic quick-start
   ```

   - Type a few messages into the console to send them to the cluster.

7. Consume these messages

   Kafka also has a command line consumer that simply prints all messages to standard output.
   ```
   bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic quick-start --from-beginning
   ```
   - Explain the order of the output.
   - Produce some more messages and check the consumer output.
   
8. Inspect the data of the Kafka topic written to disk
   - Explore the content of `/tmp/kafka-logs`. Pay special attention to the distribution of the messages within the
     different partitions.


## Level 1.2 - Understanding Replication
1. Start some more brokers
   
   Create two copies of `config/server.properties`. Make sure to specify individual values for the `broker.id` and 
   `log.dirs` properties. Also, enable the property `listeners` and use a different port for each server 
   (e.g. 9093, 9094, etc.).

   Next, start two additional brokers using these config files.
   ```
   bin/kafka-server-start.sh config/server-1.properties
   bin/kafka-server-start.sh config/server-2.properties
   ```

2. Enable topic replication

   To see replication in action, the replication factor of a topic must be two or more. Unfortunately, the
   `kafka-topics` command does not allow this modification on existing topics. The official way is to use the
   `kafka-reassign-partitions` tool as documented
   [here](https://docs.confluent.io/5.5.0/kafka/post-deployment.html#increasing-replication-factor).

   For the sake of simplicity, simply create a new topic.
   ```bash
   export BS_LIST=localhost:9092,localhost:9093,localhost:9094
   bin/kafka-topics.sh --bootstrap-server $BS_LIST --create --replication-factor 3 --partitions 3 --topic quick-start-2
   ```

3. Check the status of the new topic
   - Which broker is the leader of which partition?
   - Check the replica and ISR lists

4. Send and consume some messages using the new topic

5. Kill one broker

   ```bash
   ps aux | grep server-1.properties
   kill -9 (process_id)
   ```

   - Check the broker logging output and find information on leadership reassignment

6. Check the status of the topic
   - Which broker is the leader of which partition?
   - Check the replica and ISR lists

7. Start another console consumer and check its output

8. Send some more messages and check the consumer

9. Revive the killed broker
   - Check the logging output of all brokers

10. Send some more messages and check the consumer

## Level 1.3 - Working with consumer groups

1. Create three consumers for topic `quick-start-2` that belong to a consumer group named `group42`

   ```bash
   bin/kafka-console-consumer.sh --bootstrap-server $BS_LIST --topic quick-start-2 --group group42
   ```

2. List all existing consumer groups.

   ```bash
   bin/kafka-consumer-groups.sh --bootstrap-server $BS_LIST --list all-groups
   ```

3. Send some more messages to topic `quick-start-2`

4. Check the consumer output and the group status

   ```bash
   bin/kafka-consumer-groups.sh --bootstrap-server $BS_LIST --describe --group group42
   ```

5. Stop one consumer of `group42`

6. Check the group status of all groups. Note, how two partitions are assigned to one consumer.

7. Create another consumer group named `group23` with two consumers

8. Send some more messages to topic `quick-start-2`

9. Check the consumer output of all consumers, and the group status of all groups.

10. Stop all remaining consumers of `group42`

11. Send some more messages

12. Check the consumer output of all consumers, and the group status of all groups. Pay special attention to the
    `LAG` value of `group42`.

13. Revive all consumers of `group42`

14. Check the consumer output of all consumers, and the group status of all groups.

## Level 1.4 - More Exercises

- Use multiple producers to send messages to the same topic
- Produce messages with keys. Let consumers output the message key and observe partitioning behaviour.
- Increase the replication factor of a topic using the `kafka-reassign-partitions` tool
