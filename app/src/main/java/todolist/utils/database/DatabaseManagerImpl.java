package todolist.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import todolist.utils.loader.ConfigLoader;

public class DatabaseManagerImpl implements DatabaseManager {
  private final Map<String, String> connectionConfig;

  private final List<Connection> connectionPool; // 可用的連線
  private final List<Connection> usedConnections; // 已借出的連線
  private final int MAX_POOL_SIZE = 10;

  private Timer healthCheckTimer;

  public DatabaseManagerImpl() {
    connectionConfig = ConfigLoader.load(DatabaseManagerImpl.class,
        new String[] { "db.url", "db.user", "db.password" });

    connectionPool = new ArrayList<>();
    usedConnections = new ArrayList<>();

    // 啟動健康檢查
    startConnectionHealthCheck();

    // 在 JVM 關閉時自動清理資源
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
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

  private void startConnectionHealthCheck() {
    healthCheckTimer = new Timer(true); // Daemon thread, JVM 結束時自動終止
    healthCheckTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("Running health check");
        // 這會把 DatabaseManagerImpl 鎖住，避免其他 Thread執行此 class
        // 但是鎖整個，優化方式是可以在鎖細粒杜小一點得地方，而非整個 class
        // 可以參考 implementation v1, v2, v3 and v4
        synchronized (DatabaseManagerImpl.this) {
          connectionPool.removeIf(conn -> {
            try {
              if (conn == null || conn.isClosed()) {
                System.out.println("Found a broken connection, removing from pool.");
                return true; // 移除失效連線
              }
              return false;
            } catch (SQLException e) {
              e.printStackTrace();
              return true;
            }
          });
        }
      }
    }, 0, 30000); // 每 30 秒檢查一次
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

    // 檢查連線是否有效，若無效則重新建立連線
    try {
      if (connection == null || connection.isClosed()) {
        System.out.println("Invalid connection, creating a new one.");
        connection = createConnection();
      } else if (!connection.isValid(5)) {
        System.out.println("The connection is not valid, creating a new one.");
        closeConnection(connection);
        connection = createConnection();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Validate connection failed, creating a new one.");
      connection = createConnection(); // 如果檢查失敗則創建新連線
    }

    usedConnections.add(connection);
    return new DatabaseConnection(connection, this);
  }

  @Override
  public synchronized void releaseConnection(Connection connection) {
    System.out.println("Releasing connection: " + connection);
    if (connection != null && usedConnections.remove(connection)) {
      connectionPool.add(connection);
    }
  }

  @Override
  public int getPoolSize() {
    return connectionPool.size();
  }

  @Override
  public void shutdown() {
    healthCheckTimer.cancel();
    usedConnections.forEach(this::closeConnection);
    connectionPool.forEach(this::closeConnection);
    connectionPool.clear();
    usedConnections.clear();
    System.out.println("Connection pool has been shut down.");
  }
}
