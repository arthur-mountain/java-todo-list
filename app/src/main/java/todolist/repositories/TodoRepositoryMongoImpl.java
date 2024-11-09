package todolist.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import todolist.entities.TodoEntity;
import todolist.utils.database.MongoManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TodoRepositoryMongoImpl implements TodoRepository {
  private final MongoManager mongoManager;
  private String DATABASE_NAME;

  // 注入 MongoManager
  public TodoRepositoryMongoImpl(MongoManager mongoManager) {
    this.mongoManager = mongoManager;

    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        System.out.println("Unable to find db.properties");
        throw new RuntimeException("Mongo configuration is not set.");
      }

      properties.load(input);
      this.DATABASE_NAME = properties.getProperty("mongodb.db.name");

      if (DATABASE_NAME == null) {
        throw new RuntimeException("Mongo configuration is incomplete.");
      }
    } catch (IOException | RuntimeException ex) {
      ex.printStackTrace();
    }
  }

  private MongoCollection<Document> getTodosCollection() throws Exception {
    MongoClient client = mongoManager.getConnection();
    MongoDatabase database = client.getDatabase(DATABASE_NAME);
    return database.getCollection("todos");
  }

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

  // Create todo
  @Override
  public Optional<TodoEntity> createTodo(TodoEntity todo) {
    Document newTodoDocument = new Document("id", todo.id)
        .append("title", todo.title)
        .append("description", todo.description)
        .append("completed", todo.completed);

    try {
      getTodosCollection().insertOne(newTodoDocument);
      return Optional.of(todo);
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  // Get todos
  @Override
  public List<TodoEntity> getTodos() {
    List<TodoEntity> todos = new ArrayList<>();
    try {
      for (Document doc : getTodosCollection().find()) {
        todos.add(new TodoEntity(
            doc.getInteger("id"),
            doc.getString("title"),
            doc.getString("description"),
            doc.getBoolean("completed", false)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return todos;
  }

  @Override
  public List<TodoEntity> getTodos(Map<String, String> params) {
    List<TodoEntity> todos = new ArrayList<>();
    try {
      int page = parsePaginationOrDefault(params, "page", 1);
      int perPage = parsePaginationOrDefault(params, "per_page", 10);
      int offset = (page - 1) * perPage;

      for (Document doc : getTodosCollection().find().skip(offset).limit(perPage)) {
        todos.add(new TodoEntity(
            doc.getInteger("id"),
            doc.getString("title"),
            doc.getString("description"),
            doc.getBoolean("completed", false)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return todos;
  }

  // Get todo by id
  @Override
  public Optional<TodoEntity> getTodoById(int todoId) {
    try {
      Document doc = getTodosCollection().find(Filters.eq("id", todoId)).first();
      if (doc != null) {
        return Optional.of(new TodoEntity(
            doc.getInteger("id"),
            doc.getString("title"),
            doc.getString("description"),
            doc.getBoolean("completed", false)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  // Update todo
  @Override
  public Optional<TodoEntity> updateTodo(int todoId, TodoEntity todo) {
    Bson filter = Filters.eq("id", todoId);
    Bson updates = Updates.combine(
        Updates.set("title", todo.title),
        Updates.set("description", todo.description),
        Updates.set("completed", todo.completed));
    try {
      Document updatedDoc = getTodosCollection().findOneAndUpdate(filter, updates);
      if (updatedDoc != null) {
        return Optional.of(new TodoEntity(
            updatedDoc.getInteger("id"),
            updatedDoc.getString("title"),
            updatedDoc.getString("description"),
            updatedDoc.getBoolean("completed", false)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  // Delete todo
  @Override
  public Optional<TodoEntity> deleteTodo(int todoId) {
    try {
      Document deletedDoc = getTodosCollection().findOneAndDelete(Filters.eq("id", todoId));
      if (deletedDoc != null) {
        return Optional.of(new TodoEntity(
            deletedDoc.getInteger("id"),
            deletedDoc.getString("title"),
            deletedDoc.getString("description"),
            deletedDoc.getBoolean("completed", false)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

}
