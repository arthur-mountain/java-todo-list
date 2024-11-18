package todolist.utils.database.kafka;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import todolist.utils.loader.ConfigLoader;
import todolist.utils.logger.Logger;
import todolist.utils.logger.LoggerImpl;

public class KafkaConsumerManagerImpl implements KafkaConsumerManager {

  private final Logger logger;
  private final List<Thread> consumerThreads = new CopyOnWriteArrayList<>();

  /**
   * Initializes the Kafka consumer manager.
   */
  public KafkaConsumerManagerImpl() {
    // Initialize logger
    logger = LoggerImpl.getInstance(KafkaConsumerManagerImpl.class);

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public final void start(String topic) {
    Thread consumerThread = new Thread(() -> {
      Properties props = ConfigLoader.load(KafkaConsumerManagerImpl.class, Map.of(
          "kafka.bootstrap.servers", "bootstrap.servers",
          "kafka.group.id", "group.id",
          "kafka.key.deserializer", "key.deserializer",
          "kafka.value.deserializer", "value.deserializer")).entrySet().stream().collect(
              Properties::new,
              (prop, entry) -> prop.setProperty(entry.getKey(), entry.getValue()),
              Properties::putAll);
      KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

      consumer.subscribe(Collections.singletonList(topic));
      logger.info("Subscribed to topic: " + topic);

      try {
        while (!Thread.currentThread().isInterrupted()) {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            logger.info(String.format("Consumed message: Key = %s, Value = %s, Partition = %d, Offset = %d",
                record.key(), record.value(), record.partition(), record.offset()));
            // other logic for record if needed
          }
        }
      } catch (WakeupException e) {
        if (!Thread.currentThread().isInterrupted()) {
          logger.error("Unexpected WakeupException: " + e.getMessage());
        }
      } catch (Exception e) {
        logger.error("Error in Kafka consumer: " + e.getMessage());
      } finally {
        consumer.close();
        logger.info("Consumer closed for topic: " + topic);
      }
    });

    consumerThread.start();
    consumerThreads.add(consumerThread);
  }

  @Override
  public final void shutdown() {
    logger.info("Shutting down all consumers...");

    for (Thread thread : consumerThreads) {
      thread.interrupt();
    }

    for (Thread thread : consumerThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting for consumer threads to finish: " + e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    logger.info("All consumers shut down.");
  }
}
