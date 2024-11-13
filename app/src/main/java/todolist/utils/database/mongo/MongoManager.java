package todolist.utils.database.mongo;

import com.mongodb.client.MongoClient;

public interface MongoManager {
  MongoClient getConnection() throws Exception;

  void shutdown();
}
