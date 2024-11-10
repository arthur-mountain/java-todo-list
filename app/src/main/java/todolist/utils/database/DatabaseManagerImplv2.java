package todolist.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import todolist.utils.loader.ConfigLoader;

// 連線池版本2, 使用 synchronized List 來實作
public class DatabaseManagerImplv2 implements DatabaseManager {
  private Map<String, String> connectionConfig;

  private List<Connection> connectionPool; // 可用的連線
  private List<Connection> usedConnections; // 已借出的連線
  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 10;

  public DatabaseManagerImplv2() {
    connectionConfig = ConfigLoader.load(DatabaseManagerImpl.class,
        new String[] { "db.url", "db.user", "db.password" });

    // Diff with v1, 使用 synchronized list 來實作 connectionPool 和 usedConnections
    connectionPool = Collections.synchronizedList(new ArrayList<>());
    usedConnections = Collections.synchronizedList(new ArrayList<>());

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
  public synchronized DatabaseConnection getConnection() {
    if (connectionPool.isEmpty()) {
      if (usedConnections.size() < MAX_POOL_SIZE) {
        connectionPool.add(createConnection());
      } else {
        throw new RuntimeException("Maximum pool size reached, no available connections!");
      }
    }

    Connection connection = connectionPool.remove(connectionPool.size() - 1);
    usedConnections.add(connection);
    return new DatabaseConnection(connection, this);
  }

  @Override
  public void releaseConnection(Connection connection) {
    connectionPool.add(connection);
    usedConnections.remove(connection);
  }

  @Override
  public int getPoolSize() {
    return connectionPool.size();
  }

  @Override
  public void shutdown() {
    usedConnections.forEach(this::closeConnection);
    connectionPool.forEach(this::closeConnection);
    connectionPool.clear();
    usedConnections.clear();
  }

}
