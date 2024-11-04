package todolist.repositories;

import java.sql.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import todolist.entitys.TodoEntity;

public class TodoRepository {
  private static Connection conn;

  private static void init_db_conn() {
    if (conn != null) {
      return;
    }

    // Load the database properties
    Properties properties = new Properties();
    try (InputStream input = TodoRepository.class.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        System.out.println("Sorry, unable to find dbconfig.properties");
      } else {
        properties.load(input);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    // Database URL
    String url = properties.getProperty("db.url");

    // Database credentials
    String user = properties.getProperty("db.user");
    String password = properties.getProperty("db.password");

    if (url == null || user == null || password == null) {
      System.out.println("Database configuration is not set.");
      return;
    }

    // Connect to the database
    try {
      conn = DriverManager.getConnection(url, user, password);
      System.out.println("Connected to the PostgreSQL server successfully.");

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          if (conn != null && !conn.isClosed()) {
            conn.close();
            System.out.println("Database connection closed.");
          }
        } catch (SQLException e) {
          System.out.println("Failed to closed to PostgreSQL.");
          e.printStackTrace();
        }
      }));
    } catch (SQLException e) {
      System.out.println("Failed to connect to PostgreSQL.");
      e.printStackTrace();
    }
    // finally {
    // // Close the connection when done
    // if (conn != null) {
    // try {
    // conn.close();
    // System.out.println("Connection closed.");
    // } catch (SQLException ex) {
    // ex.printStackTrace();
    // }
    // }
    // }
  }

  // Create todo
  public Optional<TodoEntity> createTodo(TodoEntity todo) {
    init_db_conn();

    String query = "INSERT INTO todos (title, description, completed) VALUES (?, ?, ?) RETURNING *";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, todo.title);
      pstmt.setString(2, todo.description);
      pstmt.setBoolean(3, todo.completed);

      try (ResultSet rs = pstmt.executeQuery()) {
        // 如果成功插入，返回 todo；否則返回 empty optional
        return rs.next()
            ? Optional.of(new TodoEntity(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                rs.getBoolean("completed")))
            : Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return Optional.empty(); // 插入失敗時返回 empty optional
  }

  // Get todos
  private final int parsePaginationOrDefault(Map<String, String> params, String key, int defaultValue) {
    String value = params.getOrDefault(key, String.valueOf(defaultValue));
    try {
      int parsedValue = Integer.parseInt(value);
      if (parsedValue > 0) {
        return parsedValue;
      } else {
        System.out.println(key + " should be a positive integer. Using default value: " + defaultValue);
        return defaultValue;
      }
    } catch (NumberFormatException e) {
      System.out.println("Invalid format for " + key + ": " + value + ". Using default value: " + defaultValue);
      return defaultValue;
    }
  }

  public List<TodoEntity> getTodos() {
    return getTodos(Map.of());
  }

  public List<TodoEntity> getTodos(Map<String, String> params) {
    init_db_conn();

    List<TodoEntity> todos = new ArrayList<>();

    int page = parsePaginationOrDefault(params, "page", 1);
    int perPage = parsePaginationOrDefault(params, "per_page", 10);
    int offset = (page - 1) * perPage;
    String query = "SELECT * FROM todos LIMIT ? OFFSET ?";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, perPage);
      pstmt.setInt(2, offset);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          todos.add(new TodoEntity(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
              rs.getBoolean("completed")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return todos;
  }

  // Get todo by id
  public Optional<TodoEntity> getTodoById(int todoId) {
    init_db_conn();

    String query = "SELECT * FROM todos where id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, todoId);
      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next()
            ? Optional.of(new TodoEntity(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                rs.getBoolean("completed")))
            : Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return Optional.empty(); // 如果未找到記錄則返回 empty Optional
  }

  // Update todo
  public Optional<TodoEntity> updateTodo(int todoId, TodoEntity todo) {
    init_db_conn();

    String query = "UPDATE todos SET title = ?, description = ?, completed = ? WHERE id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, todo.title);
      pstmt.setString(2, todo.description);
      pstmt.setBoolean(3, todo.completed);
      pstmt.setInt(4, todoId);

      // 如果成功更新，返回更新後的 TodoEntity otherwise return empty Optional
      if (pstmt.executeUpdate() > 0) {
        return getTodoById(todoId);
      } else {
        System.out.println("No todo found with ID: " + todoId);
        return Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return Optional.empty(); // 如果未找到記錄或更新失敗，返回 empty Optional
  }

  // Delete todo
  public Optional<TodoEntity> deleteTodo(int todoId) {
    init_db_conn();

    // 查詢待刪除的記錄
    Optional<TodoEntity> optionalTodo = getTodoById(todoId);
    if (!optionalTodo.isPresent()) {
      System.out.println("No todo found with ID: " + todoId);
      return Optional.empty();
    }

    String query = "DELETE FROM todos WHERE id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, todoId);

      // 刪除成功，返回被刪除的 TodoEntity otherwise return empty optional
      if (pstmt.executeUpdate() > 0) {
        return optionalTodo;
      } else {
        System.out.println("Failed to delete todo with ID: " + todoId);
        return Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return Optional.empty(); // 刪除失敗時返回 empty optional
  }
}
