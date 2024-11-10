package todolist.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import todolist.utils.loader.ConfigLoader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 連線池版本4,
// 改用 ScheduledExecutorService 和 ReentrantLock 實作健康檢查和 lock 機制
// 加上 MAX_IDLE_TIME_MS
public class DatabaseManagerImplv4 implements DatabaseManager {
  private final Map<String, String> connectionConfig;

  private final List<Connection> connectionPool; // 可用的連線
  private final List<Connection> usedConnections; // 已借出的連線
  private final Map<Connection, Long> lastUsedTimestamps; // 跟蹤每個連線的最近使用時間
  private final int MAX_POOL_SIZE = 10;
  private final long MAX_IDLE_TIME_MS = 300000; // 閒置超過 5 分鐘自動釋放

  private final ReentrantLock lock = new ReentrantLock();
  private ScheduledExecutorService healthCheckExecutor;

  public DatabaseManagerImplv4() {
    connectionConfig = ConfigLoader.load(DatabaseManagerImplv4.class,
        new String[] { "db.url", "db.name", "db.password" });

    connectionPool = new ArrayList<>();
    usedConnections = new ArrayList<>();
    lastUsedTimestamps = new HashMap<>();

    // 啟動健康檢查
    startConnectionHealthCheck();

    // 在 JVM 關閉時自動清理資源
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private Connection createConnection() {
    try {
      return DriverManager.getConnection(connectionConfig.get("db.url"), connectionConfig.get("db.username"),
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
    healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();
    healthCheckExecutor.scheduleAtFixedRate(() -> {
      lock.lock();
      try {
        long currentTime = System.currentTimeMillis();

        // 移除無效的或超時閒置的連線
        connectionPool.removeIf(conn -> {
          try {
            if (conn == null || conn.isClosed()) {
              System.out.println("Found a broken connection, removing from pool.");
              lastUsedTimestamps.remove(conn);
              return true; // 移除失效連線
            } else if (currentTime - lastUsedTimestamps.get(conn) > MAX_IDLE_TIME_MS) {
              System.out.println("Connection idle for too long, closing.");
              closeConnection(conn);
              lastUsedTimestamps.remove(conn);
              return true; // 移除超時的閒置連線
            } else if (!conn.isValid(3)) {
              System.out.println("Connection is not valid, closing.");
              closeConnection(conn);
              lastUsedTimestamps.remove(conn);
              return true; // 移除無效連線
            }
            return false;
          } catch (SQLException e) {
            e.printStackTrace();
            closeConnection(conn);
            lastUsedTimestamps.remove(conn);
            return true;
          }
        });

      } finally {
        lock.unlock();
      }
    }, 0, 60, TimeUnit.SECONDS);
  }

  @Override
  public DatabaseConnection getConnection() {
    lock.lock();
    try {
      if (connectionPool.isEmpty()) {
        if (usedConnections.size() < MAX_POOL_SIZE) {
          Connection newConnection = createConnection();
          usedConnections.add(newConnection);
          lastUsedTimestamps.put(newConnection, System.currentTimeMillis());
          return new DatabaseConnection(newConnection, this);
        } else {
          throw new RuntimeException("Maximum pool size reached, no available connections!");
        }
      }
      //

      Connection connection = connectionPool.remove(connectionPool.size() - 1);

      // 檢查連線是否有效，若無效則重新建立連線
      try {
        if (connection == null || connection.isClosed()) {
          System.out.println("Invalid connection, creating a new one.");
          connection = createConnection();
        } else if (!connection.isValid(3)) {
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
      lastUsedTimestamps.put(connection, System.currentTimeMillis()); // 更新使用時間
      return new DatabaseConnection(connection, this);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void releaseConnection(Connection connection) {
    lock.lock();
    try {
      if (connection != null && usedConnections.remove(connection)) {
        connectionPool.add(connection);
        lastUsedTimestamps.put(connection, System.currentTimeMillis()); // 更新釋放時間
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int getPoolSize() {
    return connectionPool.size();
  }

  @Override
  public void shutdown() {
    healthCheckExecutor.shutdown();
    usedConnections.forEach(this::closeConnection);
    connectionPool.forEach(this::closeConnection);
    connectionPool.clear();
    usedConnections.clear();
    lastUsedTimestamps.clear();
    System.out.println("Connection pool has been shut down.");
  }
}
