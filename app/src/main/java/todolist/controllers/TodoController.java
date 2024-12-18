package todolist.controllers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import todolist.utils.json.Json;
import todolist.entities.TodoEntity;
import todolist.repositories.postgresql.TodoRepository;

public class TodoController implements HttpHandler {
  private final TodoRepository todoRepository;

  public TodoController(TodoRepository todoRepository) {
    this.todoRepository = todoRepository;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      List<String> contentTypeHeaders = exchange.getRequestHeaders().get("Content-Type");
      if (contentTypeHeaders == null) {
        exchange.sendResponseHeaders(400, -1);
        return;
      }
      if (!contentTypeHeaders.stream().anyMatch(header -> header.contains("application/json"))) {
        exchange.sendResponseHeaders(400, -1);
        return;
      }
      switch (exchange.getRequestMethod()) {
        case "GET":
          System.out.println("GET: \n" + exchange);
          handleGet(exchange);
          break;
        case "POST":
          System.out.println("POST: \n" + exchange);
          handlePost(exchange);
          break;
        case "PATCH":
          System.out.println("PATCH: \n" + exchange);
          handlePatch(exchange);
          break;
        case "DELETE":
          System.out.println("DELETE: \n" + exchange);
          handleDelete(exchange);
          break;
        default:
          System.out.println("Method Not Allowed");
          exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
      }
    } finally {
      // Ensure HttpExchange 中的 InputStream 和 OutputStream 資源被釋放
      exchange.close();
    }
  }

  // GET，列出所有待辦事項 or 通過 ID 獲取單一待辦
  private void handleGet(HttpExchange exchange) throws IOException {
    Integer todoId = null;
    try {
      String path = exchange.getRequestURI().getPath();
      String _todoId = path.substring(path.lastIndexOf('/') + 1);
      if (_todoId.matches("\\d+")) {
        todoId = Integer.parseInt(_todoId);
      }
    } catch (Exception e) {
      e.printStackTrace();
      todoId = null;
    }

    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "application/json; charset=UTF-8");
    byte[] responseBytes;
    if (todoId == null) {
      responseBytes = Json.toBytes(todoRepository.getTodos(parseQuery(exchange.getRequestURI().getQuery())));
    } else {
      Optional<TodoEntity> todo = todoRepository.getTodoById(todoId);
      if (todo.isPresent()) {
        responseBytes = Json.toBytes(todo.get());
      } else {
        responseBytes = Json.toBytes(new HashMap<>(Map.of("message", "Not found todo with id -> " + todoId)));
      }
    }
    exchange.sendResponseHeaders(200, responseBytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(responseBytes);
    }
  }

  // POST，新增待辦事項
  private void handlePost(HttpExchange exchange) throws IOException {
    try (InputStream input = exchange.getRequestBody()) {
      Optional<TodoEntity> createdTodo = todoRepository
          .createTodo(Json.fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (createdTodo.isPresent()) {
        responseBytes = Json.toBytes(createdTodo.get());
        exchange.sendResponseHeaders(201, responseBytes.length);
      } else {
        responseBytes = Json.toBytes(new HashMap<>(Map.of("message", "Create todo failed")));
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = Json.toBytes(new HashMap<>(Map.of("message", "Missing todo item")));
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }

  // PATCH，更新待辦事項
  private void handlePatch(HttpExchange exchange) throws IOException {
    int todoId;
    try {
      String path = exchange.getRequestURI().getPath();
      todoId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      e.printStackTrace();
      byte[] responseBytes = Json.toBytes(
          new HashMap<>(Map.of("message", "Missing todo id or invalid format")));
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
      return;
    }

    if (!todoRepository.getTodoById(todoId).isPresent()) {
      byte[] responseBytes = Json.toBytes(
          new HashMap<>(Map.of("message", "Missing todo id or todo item not found")));
      exchange.sendResponseHeaders(404, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
      return;
    }

    try (InputStream input = exchange.getRequestBody()) {
      Optional<TodoEntity> updatedTodo = todoRepository.updateTodo(todoId,
          Json.fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (updatedTodo.isPresent()) {
        responseBytes = Json.toBytes(updatedTodo.get());
        exchange.sendResponseHeaders(200, responseBytes.length);
      } else {
        responseBytes = Json.toBytes(
            new HashMap<>(Map.of("message", "Update todo failed with id -> " + todoId)));
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = Json.toBytes(new HashMap<>(Map.of("message", "Not found todo item ")));
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }

  // DELETE，刪除指定的待辦事項
  private void handleDelete(HttpExchange exchange) throws IOException {
    int todoId;
    try {
      String path = exchange.getRequestURI().getPath();
      todoId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = Json.toBytes(
            new HashMap<>(Map.of("message", "Missing todo id or invalid format")));
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    Optional<TodoEntity> deletedTodo = todoRepository.deleteTodo(todoId);

    byte[] responseBytes;
    if (deletedTodo.isPresent()) {
      responseBytes = Json.toBytes(deletedTodo.get());
      exchange.sendResponseHeaders(200, responseBytes.length);
    } else {
      responseBytes = Json.toBytes(
          new HashMap<>(Map.of("message", "Not found todo item with id -> " + todoId)));
      exchange.sendResponseHeaders(404, responseBytes.length);
    }

    try (OutputStream os = exchange.getResponseBody()) {
      os.write(responseBytes);
    }
  }

  // (unused) URL query string parser
  // Example:
  //
  // String query = exchange.getRequestURI().getQuery();
  // Map<String, String> params = parseQuery(query);
  // String todoId = params.get("id");
  public static Map<String, String> parseQuery(String query) {
    Map<String, String> params = new HashMap<>();

    if (query == null || query.isEmpty()) {
      return params;
    }

    String[] pairs = query.split("&");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2); // Limit split to 2 parts
      String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
      String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
      params.put(key, value);
    }

    return params;
  }

  // (unused) actually just for see binary of string
  static void printBinary(String str) {
    System.out.println("original string -> " + str);
    StringBuilder binaryString = new StringBuilder();
    for (byte b : str.getBytes()) {
      binaryString.append(String.format("%8s", Integer.toBinaryString(b &
          0xFF)).replace(' ', '0'));
    }
    System.out.println("binary string -> " + binaryString.toString());
  }
}
