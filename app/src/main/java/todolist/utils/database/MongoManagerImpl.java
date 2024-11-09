package todolist.utils.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoManagerImpl implements MongoManager {
  private String url;

  private final MongoClient mongoClient;

  public MongoManagerImpl() {
    loadDatabaseConfig();

    // Create a new MongoClient
    this.mongoClient = MongoClients.create(url);

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public MongoClient getConnection() {
    return mongoClient;
  }

  @Override
  public void shutdown() {
    closeMongoClient();
    System.out.println("MongoClient has been shut down.");
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

  private void closeMongoClient() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }
}
