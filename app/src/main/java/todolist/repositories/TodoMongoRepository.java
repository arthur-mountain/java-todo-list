package todolist.repositories;

import org.bson.types.ObjectId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import todolist.entities.TodoMongoEntity;

public interface TodoMongoRepository {
  // Create todo
  Optional<TodoMongoEntity> createTodo(TodoMongoEntity todo);

  // Get todos
  List<TodoMongoEntity> getTodos();

  // Get todos by params
  List<TodoMongoEntity> getTodos(Map<String, String> params);

  // Get todo by id
  Optional<TodoMongoEntity> getTodoById(ObjectId todoId);

  // Update todo
  Optional<TodoMongoEntity> updateTodo(ObjectId todoId, TodoMongoEntity todo);

  // Delete todo
  Optional<TodoMongoEntity> deleteTodo(ObjectId todoId);
}
