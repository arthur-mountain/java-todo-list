package todolist.utils.database.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import todolist.utils.loader.ConfigLoader;
import todolist.utils.logger.LoggerImpl;

// TODO: Fix kafka consumer is no thread safe error
public class KafkaConsumerManagerImpl implements KafkaConsumerManager {
  private volatile boolean running = true;
  private final KafkaConsumer<String, String> consumer;
  private final LoggerImpl logger;

  /**
   * Initializes the Kafka consumer with the provided configurations.
   */
  public KafkaConsumerManagerImpl() {
    // Initialize logger
    logger = LoggerImpl.getInstance(KafkaConsumerManagerImpl.class);

    // Load configuration and remap keys
    Properties props = ConfigLoader.load(KafkaProducerManagerImpl.class, Map.of(
        "kafka.bootstrap.servers", "bootstrap.servers",
        "kafka.group.id", "group.id",
        "kafka.key.deserializer", "key.deserializer",
        "kafka.value.deserializer", "value.deserializer")).entrySet().stream().collect(
            Properties::new,
            (prop, entry) -> prop.setProperty(entry.getKey(), entry.getValue()),
            Properties::putAll);
    consumer = new KafkaConsumer<>(props);

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public final void start(String topic) {
    consumer.subscribe(Collections.singletonList(topic));
    logger.info("Subscribed to topic: " + topic);

    // Consumer loop
    // 這邊會一直 poll kafka meesges，直到 running 變成 false，
    // 但這樣其他地方會沒辦法 start another topic，目前只是先測試用 logging messages
    try {
      while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          logger.info(String.format("Consumed message: Key = %s, Value = %s, Partition = %d, Offset = %d",
              record.key(), record.value(), record.partition(), record.offset()));
        }
      }
    } catch (Exception e) {
      logger.error("Error in Kafka consumer: " + e.getMessage());
    } finally {
      shutdown();
    }
  }

  @Override
  public final void shutdown() {
    running = false;
    if (consumer != null) {
      consumer.close();
    }
  }
}
