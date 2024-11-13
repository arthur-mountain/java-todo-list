package todolist.utils.database.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import todolist.utils.loader.ConfigLoader;

public class MongoManagerImpl implements MongoManager {
  private final String url;

  private final MongoClient mongoClient;

  public MongoManagerImpl() {
    url = ConfigLoader.load(MongoManagerImpl.class, "mongodb.url");

    // Create a new MongoClient
    mongoClient = MongoClients.create(url);

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

  private void closeMongoClient() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }
}
