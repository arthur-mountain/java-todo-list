package todolist.controllers;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import todolist.entitys.TodoEntity;
import todolist.repositories.TodoRepository;

public class TodoController implements HttpHandler {

  private static final TodoRepository todoRepository = new TodoRepository();

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

  private <T> String toJSON(T values) {
    Gson gson = new Gson();
    return gson.toJson(values);
  }

  private <T> T fromJSON(String json, Class<T> type) {
    Gson gson = new Gson();
    return gson.fromJson(json, type);
  }

  private byte[] toBytes(String str) {
    return str.getBytes(StandardCharsets.UTF_8);
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

    System.out.println("todoId -> " + todoId);

    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "application/json; charset=UTF-8");
    byte[] responseBytes;
    if (todoId == null) {
      responseBytes = toJSON(todoRepository.getTodos()).getBytes(StandardCharsets.UTF_8);
    } else if (todoRepository.getTodoById(todoId).isPresent()) {
      responseBytes = toJSON(todoRepository.getTodoById(todoId).get()).getBytes(StandardCharsets.UTF_8);
    } else {
      responseBytes = toJSON(new HashMap<>(Map.of("message", "Not found todo with id -> " + todoId)))
          .getBytes(StandardCharsets.UTF_8);
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
          .createTodo(fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (createdTodo.isPresent()) {
        responseBytes = toBytes(toJSON(createdTodo.get()));
        exchange.sendResponseHeaders(201, responseBytes.length);
      } else {
        responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Create todo failed"))));
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Missing todo item"))));
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
      byte[] responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Missing todo id or invalid format"))));
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
      return;
    }

    if (!todoRepository.getTodoById(todoId).isPresent()) {
      byte[] responseBytes = toBytes(
          toJSON(new HashMap<>(Map.of("message", "Missing todo id or todo item not found"))));
      exchange.sendResponseHeaders(404, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
      return;
    }

    try (InputStream input = exchange.getRequestBody()) {
      Optional<TodoEntity> updatedTodo = todoRepository.updateTodo(todoId,
          fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (updatedTodo.isPresent()) {
        responseBytes = toBytes(toJSON(toJSON(updatedTodo.get())));
        exchange.sendResponseHeaders(200, responseBytes.length);
      } else {
        responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Update todo failed with id -> " + todoId))));
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Not found todo item "))));
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
        byte[] responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Missing todo id or invalid format"))));
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    Optional<TodoEntity> deletedTodo = todoRepository.deleteTodo(todoId);

    byte[] responseBytes;
    if (deletedTodo.isPresent()) {
      responseBytes = toBytes(toJSON(deletedTodo.get()));
      exchange.sendResponseHeaders(200, responseBytes.length);
    } else {
      responseBytes = toBytes(toJSON(new HashMap<>(Map.of("message", "Not found todo item with id -> " + todoId))));
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
  static Map<String, String> parseQuery(String query) {
    Map<String, String> params = new HashMap<>();

    if (query == null) {
      return params;
    }

    String[] pairs = query.split("&");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=");
      if (keyValue.length == 2) {
        params.put(keyValue[0], keyValue[1]);
      }
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
