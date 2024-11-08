package todolist.utils.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

// 連線池版本2, 使用 synchronized List 來實作
public class DatabaseManagerImplv2 implements DatabaseManager {
  private String url;
  private String username;
  private String password;

  private List<Connection> connectionPool; // 可用的連線
  private List<Connection> usedConnections; // 已借出的連線
  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MAX_POOL_SIZE = 10;

  public DatabaseManagerImplv2() {
    loadDatabaseConfig();
    connectionPool = Collections.synchronizedList(new ArrayList<>());
    usedConnections = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      connectionPool.add(createConnection());
    }
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
