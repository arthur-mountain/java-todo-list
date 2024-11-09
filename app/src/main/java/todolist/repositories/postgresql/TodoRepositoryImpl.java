package todolist.repositories.postgresql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import todolist.entities.TodoEntity;
import todolist.utils.database.DatabaseManager;

public class TodoRepositoryImpl implements TodoRepository {
  private final DatabaseManager databaseManager;

  // 注入 DatabaseManager
  public TodoRepositoryImpl(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  // Create todo
  @Override
  public Optional<TodoEntity> createTodo(TodoEntity todo) {
    Connection connection;
    try {
      connection = databaseManager.getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }

    String query = "INSERT INTO todos (title, description, completed) VALUES (?, ?, ?) RETURNING *";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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
    } finally {
      databaseManager.releaseConnection(connection);
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

  @Override
  public List<TodoEntity> getTodos() {
    return getTodos(Map.of());
  }

  @Override
  public List<TodoEntity> getTodos(Map<String, String> params) {
    List<TodoEntity> todos = new ArrayList<>();

    Connection connection;
    try {
      connection = databaseManager.getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return todos;
    }

    int page = parsePaginationOrDefault(params, "page", 1);
    int perPage = parsePaginationOrDefault(params, "per_page", 10);
    int offset = (page - 1) * perPage;
    String query = "SELECT * FROM todos LIMIT ? OFFSET ?";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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
    } finally {
      databaseManager.releaseConnection(connection);
    }

    return todos;
  }

  // Get todo by id
  @Override
  public Optional<TodoEntity> getTodoById(int todoId) {
    Connection connection;
    try {
      connection = databaseManager.getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }

    String query = "SELECT * FROM todos where id = ?";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setInt(1, todoId);
      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next()
            ? Optional.of(new TodoEntity(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                rs.getBoolean("completed")))
            : Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      databaseManager.releaseConnection(connection);
    }

    return Optional.empty(); // 如果未找到記錄則返回 empty Optional
  }

  // Update todo;
  @Override
  public Optional<TodoEntity> updateTodo(int todoId, TodoEntity todo) {
    Connection connection;
    try {
      connection = databaseManager.getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }

    String query = "UPDATE todos SET title = ?, description = ?, completed = ? WHERE id = ?";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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
    } finally {
      databaseManager.releaseConnection(connection);
    }

    return Optional.empty(); // 如果未找到記錄或更新失敗，返回 empty Optional
  }

  // Delete todo
  @Override
  public Optional<TodoEntity> deleteTodo(int todoId) {
    // 查詢待刪除的記錄
    Optional<TodoEntity> optionalTodo = getTodoById(todoId);
    if (!optionalTodo.isPresent()) {
      System.out.println("No todo found with ID: " + todoId);
      return Optional.empty();
    }

    Connection connection;
    try {
      connection = databaseManager.getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }

    String query = "DELETE FROM todos WHERE id = ?";

    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
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
    } finally {
      databaseManager.releaseConnection(connection);
    }

    return Optional.empty(); // 刪除失敗時返回 empty optional
  }
}
