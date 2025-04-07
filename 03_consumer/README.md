# 3 - Consuming Events

## Level 3.1 - Introduction to Consumers

For starters, let's implement a consumer that simply prints the content of a
topic to the console - similar to the functionality of the `kafka-console-consumer`.

### Learning Objectives

- creating consumer applications
- understanding consumer offset management

### Tasks

1. Add some basic logic to the `SimpleConsumerApp` and let it consume the content of the `numbers` topic
   (see exercise 2.1).
2. Set property `auto.offset.reset` to `earliest` to allow consuming messages that have been produced before the
   consumer was started.
3. Start the application and check its output. Wait until all numbers have been consumed.
4. Stop and restart the consumer application. **Why does the consumer not print any messages, despite property
   `auto.offset.reset` being set to `earliest`? How to fix that?**

## Level 3.2 - Understanding Consumer Groups

Usually, producing messages is much faster than consuming and processing them. By distributing messages to different
partitions, Kafka allows us to use multiple consumers to process messages in parallel and scale the consumer side.
For this, we must enable the consumer to join a group.

### Learning Objectives

- speed up processing of large topics by grouping multiple consumers
- seeing multiple consumers process messages at their own pace
- understanding the root cause of consumer lags and how to deal with them
- identifying the source of partition rebalances

### Tasks

1. Update the `SimpleConsumerApp` to allow multiple instances to work together in groups.
2. To make the effects of multiple consumers working together more visible, add a sleeping period (e.g. 250 ms)
   before consuming the next messages. This simulates the computation work a regular consumer would have to do.
3. Update the `SimpleProducerApp` to continuously send random numbers to the `numbers` topic. 
4. Start several fast and slow consumers in different groups to consume these messages. Use the `kafka-consumer-groups`
   command to monitor the consumer offsets and lags of the groups.
5. Dynamically add and remove group members by starting and stopping consumer instances. Observe the rebalancing
   behaviour by checking the broker logging output and the partition assignments as shown by the
   `kafka-consumer-groups` command.

## Level 3.3 - Static Group Membership

To reduce consumer rebalances due to transient failures, Apache Kafka introduced the concept of static membership.
The main idea behind static membership is that each consumer instance is attached to a unique identifier configured
with `group.instance.id`. If a consumer is restarted or killed due to a transient failure, the broker coordinator
will not inform other consumers that a rebalance is necessary until `session.timeout.ms` is reached and thus other
consumers may continue consuming messages without interruption. Let's see this in action!

### Learning Objectives

- minimizing the number of partition rebalances
- understanding the consequences of messing with the rebalancing protocol

### Tasks

1. Update the `SimpleConsumerApp` created in the previous level and by adding the consumer property `group.instance.id`.
   The value of that parameter is supposed to be passed as an argument to the application. Also, set
   `session.timeout.ms` to 60 seconds, so we have enough time to do some manual analysis.
2. Start three instances of the updated consumer that work together in a group. Make sure to use individual static
   group membership ids.
3. Use the `kafka-consumer-groups` command to monitor the partition assignments of the consumers.
4. Stop one consumer. Check the partition assignments of the consumers again. All partitions are still assigned to a
   consumer, but some partitions are not being read from anymore, i.e. their "current offset" does not change. That is
   because the death of the consumer did not trigger a partition rebalance immediately.
5. Revive the dead consumer. Check the broker logging output and the partition assignments of the consumers. The
   revived consumer should be assigned to the same partition as before. The other two should not have been
   interrupted at all.
6. Stop another consumer and wait for the session to time out. Check the broker logging output and the partition
   assignments of the consumers again. A regular partition rebalancing should have taken place.

### Bonus

- What would happen if the dead consumer would be revived and killed over and over again?
