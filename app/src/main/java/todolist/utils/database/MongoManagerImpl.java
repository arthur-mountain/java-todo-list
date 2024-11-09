package todolist.utils.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoManagerImpl implements MongoManager {
  private String url;

  private final List<MongoClient> mongoClientPool; // Available connections
  private final List<MongoClient> usedMongoClients; // Connections in use
  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 10;

  private Timer healthCheckTimer;

  public MongoManagerImpl() {
    loadDatabaseConfig();
    mongoClientPool = new ArrayList<>(INITIAL_POOL_SIZE);
    usedMongoClients = new ArrayList<>();

    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      mongoClientPool.add(createMongoClient());
    }

    // Start health check
    startMongoClientHealthCheck();

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private void loadDatabaseConfig() {
    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        System.out.println("Unable to find db.properties");
        throw new RuntimeException("Mongo configuration is not set.");
      }

      properties.load(input);
      url = properties.getProperty("mongodb.url");

      if (url == null) {
        throw new RuntimeException("Mongo configuration is incomplete.");
      }
    } catch (IOException | RuntimeException ex) {
      ex.printStackTrace();
    }
  }

  private MongoClient createMongoClient() {
    return MongoClients.create(url);
  }

  private void closeMongoClient(MongoClient client) {
    if (client != null) {
      client.close();
    }
  }

  private void startMongoClientHealthCheck() {
    healthCheckTimer = new Timer(true); // Daemon thread, will stop with JVM
    healthCheckTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        synchronized (MongoManagerImpl.this) {
          mongoClientPool.removeIf(client -> {
            try {
              if (client == null) {
                System.out.println("Found a broken connection, removing from pool.");
                return true; // Remove invalid connection
              }
              return false;
            } catch (Exception e) {
              e.printStackTrace();
              return true;
            }
          });

          // Replenish pool if below initial size
          while (mongoClientPool.size() < INITIAL_POOL_SIZE) {
            mongoClientPool.add(createMongoClient());
            System.out.println("Added a new connection to the pool.");
          }
        }
      }
    }, 0, 60000); // Check every 60 seconds
  }

  @Override
  public synchronized MongoClient getConnection() {
    if (mongoClientPool.isEmpty()) {
      if (usedMongoClients.size() < MAX_POOL_SIZE) {
        mongoClientPool.add(createMongoClient());
      } else {
        throw new RuntimeException("Maximum pool size reached, no available connections!");
      }
    }

    MongoClient client = mongoClientPool.remove(mongoClientPool.size() - 1);
    usedMongoClients.add(client);
    return client;
  }

  @Override
  public synchronized void releaseConnection(MongoClient client) {
    if (client != null && usedMongoClients.remove(client)) {
      mongoClientPool.add(client);
    }
  }

  @Override
  public int getPoolSize() {
    return mongoClientPool.size();
  }

  @Override
  public void shutdown() {
    healthCheckTimer.cancel();
    usedMongoClients.forEach(this::closeMongoClient);
    mongoClientPool.forEach(this::closeMongoClient);
    mongoClientPool.clear();
    usedMongoClients.clear();
    System.out.println("MongoClient pool has been shut down.");
  }
}
