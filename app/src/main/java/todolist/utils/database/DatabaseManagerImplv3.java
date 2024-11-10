package todolist.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import todolist.utils.loader.ConfigLoader;

// 連線池版本3, 使用 concurrent BlockingQueue 來實作
public class DatabaseManagerImplv3 implements DatabaseManager {
  private final Map<String, String> connectionConfig;

  private final BlockingQueue<Connection> connectionPool;
  private final int INITIAL_POOL_SIZE = 5;
  private final int MAX_POOL_SIZE = 10;

  public DatabaseManagerImplv3() {
    connectionConfig = ConfigLoader.load(DatabaseManagerImpl.class,
        new String[] { "db.url", "db.user", "db.password" });

    connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    // 初始化 connect pool
    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      connectionPool.add(createConnection());
    }
  }

  private Connection createConnection() {
    try {
      return DriverManager.getConnection(connectionConfig.get("db.url"), connectionConfig.get("db.user"),
          connectionConfig.get("db.password"));
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to create a database connection.");
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
      // connectionPool.take -> 如果沒有可用連線，則等待(blocked)直到有可用的連線
      return new DatabaseConnection(connectionPool.take(), this);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for a connection from the pool.", e);
    }
  }

  @Override
  public void releaseConnection(Connection connection) {
    try {
      // 如果連線池已滿，直接關閉多餘的連線，否則將連線歸還到連線池中
      if (connectionPool.size() < MAX_POOL_SIZE) {
        connectionPool.put(connection);
      } else {
        connection.close();
      }
    } catch (InterruptedException | SQLException e) {
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
    // 關閉所有連線
    connectionPool.forEach(this::closeConnection);
    connectionPool.clear();
  }
}
