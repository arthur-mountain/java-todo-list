package todolist.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import todolist.utils.loader.ConfigLoader;

// 連線池版本3, 使用 concurrent BlockingQueue 來實作
// 這邊目前採用預先建立全部連線，可能對一開始啟動負擔比較大，
// 但可以避免在使用時才建立連線的開銷，
// 可以看要怎麼取捨，或者則中，先建立一半連線，之後再依需求建立連線
public class DatabaseManagerImplv3 implements DatabaseManager {
  private final Map<String, String> connectionConfig;
  private final BlockingQueue<Connection> connectionPool;
  private final int MAX_POOL_SIZE = 10;

  public DatabaseManagerImplv3() {
    connectionConfig = ConfigLoader.load(DatabaseManagerImplv3.class,
        new String[] { "db.url", "db.user", "db.password" });
    connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    // Initialize the pool with pre-created connections
    for (int i = 0; i < MAX_POOL_SIZE; i++) {
      connectionPool.add(createConnection());
    }
  }

  private Connection createConnection() {
    try {
      return DriverManager.getConnection(
          connectionConfig.get("db.url"),
          connectionConfig.get("db.user"),
          connectionConfig.get("db.password"));
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create a database connection.", e);
    }
  }

  private void closeConnection(Connection connection) {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public DatabaseConnection getConnection() {
    try {
      // Block if no available connections
      return new DatabaseConnection(connectionPool.take(), this);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for a connection from the pool.", e);
    }
  }

  @Override
  public void releaseConnection(Connection connection) {
    try {
      if (connectionPool.size() < MAX_POOL_SIZE) {
        connectionPool.put(connection);
      } else {
        closeConnection(connection); // Close excess connections if the pool is full
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }
  }

  @Override
  public int getPoolSize() {
    return connectionPool.size();
  }

  @Override
  public void shutdown() {
    // Close all connections in the pool
    connectionPool.forEach(this::closeConnection);
    connectionPool.clear();
  }
}
