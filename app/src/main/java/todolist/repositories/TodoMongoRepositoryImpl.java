package todolist.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import todolist.entities.TodoMongoEntity;
import todolist.utils.database.MongoManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TodoMongoRepositoryImpl implements TodoMongoRepository {
  private final MongoManager mongoManager;
  private String DATABASE_NAME;

  // 注入 MongoManager
  public TodoMongoRepositoryImpl(MongoManager mongoManager) {
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
  public Optional<TodoMongoEntity> createTodo(TodoMongoEntity todo) {
    Document newTodoDocument = new Document("title", todo.title)
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
  public List<TodoMongoEntity> getTodos() {
    List<TodoMongoEntity> todos = new ArrayList<>();
    try {
      for (Document doc : getTodosCollection().find()) {
        todos.add(new TodoMongoEntity(
            doc.getObjectId("_id"),
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
  public List<TodoMongoEntity> getTodos(Map<String, String> params) {
    List<TodoMongoEntity> todos = new ArrayList<>();
    try {
      int page = parsePaginationOrDefault(params, "page", 1);
      int perPage = parsePaginationOrDefault(params, "per_page", 10);
      int offset = (page - 1) * perPage;

      for (Document doc : getTodosCollection().find().skip(offset).limit(perPage)) {
        todos.add(new TodoMongoEntity(
            doc.getObjectId("_id"),
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
  public Optional<TodoMongoEntity> getTodoById(ObjectId todoId) {
    try {
      Document doc = getTodosCollection().find(Filters.eq("_id", todoId)).first();
      if (doc != null) {
        return Optional.of(new TodoMongoEntity(
            doc.getObjectId("_id"),
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
  public Optional<TodoMongoEntity> updateTodo(ObjectId todoId, TodoMongoEntity todo) {
    Bson filter = Filters.eq("_id", todoId);
    Bson updates = Updates.combine(
        Updates.set("title", todo.title),
        Updates.set("description", todo.description),
        Updates.set("completed", todo.completed));
    try {
      Document updatedDoc = getTodosCollection().findOneAndUpdate(filter, updates);
      if (updatedDoc != null) {
        return Optional.of(new TodoMongoEntity(
            updatedDoc.getObjectId("_id"),
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
  public Optional<TodoMongoEntity> deleteTodo(ObjectId todoId) {
    try {
      Document deletedDoc = getTodosCollection().findOneAndDelete(Filters.eq("_id", todoId));
      if (deletedDoc != null) {
        return Optional.of(new TodoMongoEntity(
            deletedDoc.getObjectId("_id"),
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
