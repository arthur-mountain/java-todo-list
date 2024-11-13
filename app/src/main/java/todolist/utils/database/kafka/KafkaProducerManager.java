package todolist.utils.database.kafka;

public interface KafkaProducerManager {

  /**
   * Sends a message to the specified Kafka topic.
   *
   * @param topic The Kafka topic to send the message to.
   * @param key   The key of the message (can be null).
   * @param value The value of the message.
   */
  void sendMessage(String topic, String key, String value);

  void shutdown();
}
