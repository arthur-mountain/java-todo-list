package todolist.utils.database;

import com.mongodb.client.MongoClient;

public interface MongoManager {
  MongoClient getConnection() throws Exception;

  void shutdown();
}
