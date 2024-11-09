package todolist.utils.database;

import com.mongodb.client.MongoClient;

public interface MongoManager {
  MongoClient getConnection() throws Exception;

  void releaseConnection(MongoClient connection);

  void shutdown();

  int getPoolSize();
}
