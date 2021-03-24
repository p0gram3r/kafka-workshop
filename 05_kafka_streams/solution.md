## level 5.4
```
# creating topics
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --partitions 3 --topic movies
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --partitions 3 --topic ratings
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --partitions 3 --topic rated-movies
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --partitions 3 --topic movies-rekeyed

# producing movies
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic movies
  
# producing ratings
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic ratings
 
# checking output
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --property print.key=true \
  --property key.separator=: \
  --topic rated-movies \
  --from-beginning
```