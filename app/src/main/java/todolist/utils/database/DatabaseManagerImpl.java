package todolist.utils.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DatabaseManagerImpl implements DatabaseManager {
  private String url;
  private String username;
  private String password;

  private List<Connection> connectionPool; // 可用的連線
  private List<Connection> usedConnections; // 已借出的連線
  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 10;

  private Timer healthCheckTimer;

  public DatabaseManagerImpl() {
    loadDatabaseConfig();
    connectionPool = new ArrayList<>(INITIAL_POOL_SIZE);
    usedConnections = new ArrayList<>();

    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      connectionPool.add(createConnection());
    }

    // 啟動健康檢查
    startConnectionHealthCheck();

    // 在 JVM 關閉時自動清理資源
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private void loadDatabaseConfig() {
    Properties properties = new Properties();
    try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        System.out.println("Sorry, unable to find db.properties");
        throw new RuntimeException("Database configuration is not set.");
      }

      properties.load(input);
      url = properties.getProperty("db.url");
      username = properties.getProperty("db.user");
      password = properties.getProperty("db.password");

      if (url == null || username == null || password == null) {
        throw new RuntimeException("Database configuration is not fully set.");
      }
    } catch (IOException | RuntimeException ex) {
      ex.printStackTrace();
    }
  }

  private Connection createConnection() {
    try {
      return DriverManager.getConnection(url, username, password);
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
        // 這會把 DatabaseManagerImpl 鎖住，避免其他 Thread執行此 class
        // 但是鎖整個，優化方式是可以在鎖細粒杜小一點得地方，而非整個 class
        // 可以參考 implementation v1 and v2
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

          // 如果池中連線不足，補充新的連線
          while (connectionPool.size() < INITIAL_POOL_SIZE) {
            connectionPool.add(createConnection());
            System.out.println("Added a new connection to the pool.");
          }
        }
      }
    }, 0, 60000); // 每 60 秒檢查一次
  }

  @Override
  public synchronized Connection getConnection() {
    if (connectionPool.isEmpty()) {
      if (usedConnections.size() < MAX_POOL_SIZE) {
        connectionPool.add(createConnection());
      } else {
        throw new RuntimeException("Maximum pool size reached, no available connections!");
      }
    }

    Connection connection = connectionPool.remove(connectionPool.size() - 1);
    usedConnections.add(connection);
    return connection;
  }

  @Override
  public synchronized void releaseConnection(Connection connection) {
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
    System.out.println("Connection pool has been shut down.");
  }
}
