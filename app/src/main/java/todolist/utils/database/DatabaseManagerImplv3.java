package todolist.utils.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// 連線池版本3, 使用 concurrent BlockingQueue 來實作
public class DatabaseManagerImplv3 implements DatabaseManager {
  private String url;
  private String username;
  private String password;

  private BlockingQueue<Connection> connectionPool;
  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 10;

  public DatabaseManagerImplv3() {
    loadDatabaseConfig();
    connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    // 初始化連線池
    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      connectionPool.add(createConnection());
    }
  }

  private void loadDatabaseConfig() {
    Properties properties = new Properties();
    try (InputStream input = DatabaseManagerImplv3.class.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        System.out.println("Sorry, unable to find db.properties");
        throw new RuntimeException("Database configuration is not set.");
      }
      properties.load(input);

      url = properties.getProperty("db.url");
      username = properties.getProperty("db.user");
      password = properties.getProperty("db.password");

      if (url == null || username == null || password == null) {
        System.out.println("Database configuration is not fully set.");
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

  @Override
  public Connection getConnection() {
    try {
      // 如果沒有可用連線，則等待直到有可用的連線
      return connectionPool.take();
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

  private void closeConnection(Connection connection) {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
