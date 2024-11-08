package todolist.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import todolist.entities.TodoEntity;

public interface TodoRepository {
  // Create todo
  Optional<TodoEntity> createTodo(TodoEntity todo);

  // Get todos
  List<TodoEntity> getTodos();

  // Get todos by params
  List<TodoEntity> getTodos(Map<String, String> params);

  // Get todo by id
  Optional<TodoEntity> getTodoById(int todoId);

  // Update todo
  Optional<TodoEntity> updateTodo(int todoId, TodoEntity todo);

  // Delete todo
  Optional<TodoEntity> deleteTodo(int todoId);
}
