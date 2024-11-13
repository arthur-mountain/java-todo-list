package todolist.utils.database.kafka;

public interface KafkaConsumerManager {

  /**
   * Starts the Kafka consumer to listen to the given topic.
   *
   * @param topic The Kafka topic to consume messages from.
   */
  void start(String topic);

  /**
   * Shuts down the Kafka consumer gracefully.
   */
  void shutdown();
}
