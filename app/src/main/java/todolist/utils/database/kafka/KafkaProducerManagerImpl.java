package todolist.utils.database.kafka;

import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import todolist.utils.loader.ConfigLoader;
import todolist.utils.logger.Logger;
import todolist.utils.logger.LoggerImpl;

// TODO: Fix kafka producer is no thread safe error
public class KafkaProducerManagerImpl implements KafkaProducerManager {

  private final KafkaProducer<String, String> producer;
  private final Logger logger;

  /**
   * Initializes the Kafka producer with the provided configurations.
   * 
   * @param bootstrapServers The Kafka bootstrap server address.
   */
  public KafkaProducerManagerImpl() {
    // Initialize logger
    logger = LoggerImpl.getInstance(KafkaProducerManagerImpl.class);

    // Load configuration and remap keys
    Properties props = ConfigLoader.load(KafkaProducerManagerImpl.class, Map.of(
        "kafka.bootstrap.servers", "bootstrap.servers",
        "kafka.group.id", "group.id",
        "kafka.key.serializer", "key.serializer",
        "kafka.value.serializer", "value.serializer")).entrySet().stream().collect(
            Properties::new,
            (prop, entry) -> prop.setProperty(entry.getKey(), entry.getValue()),
            Properties::putAll);
    producer = new KafkaProducer<>(props);

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public final void sendMessage(String topic, String key, String value) {
    try {
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
      producer.send(record, (metadata, exception) -> {
        if (exception != null) {
          logger.error(String.format("Error sending message to topic %s: %s%n", topic,
              exception.getMessage()));
        } else {
          logger.info(String.format("Message sent to topic %s: Partition = %d, Offset = %d%n",
              metadata.topic(), metadata.partition(), metadata.offset()));
        }
      });
    } catch (Exception e) {
      logger.error(String.format("Failed to send message to topic %s: %s%n", topic, e.getMessage()));
    }
  }

  @Override
  public final void shutdown() {
    if (producer != null) {
      producer.close();
      System.out.println("Shutting down Kafka producer...\n");
    }
  }
}
