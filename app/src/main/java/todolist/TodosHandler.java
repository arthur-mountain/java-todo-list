package todolist;

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

public class TodosHandler implements HttpHandler {

  private static final TodosRepository todosRepository = new TodosRepository();

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

  // GET，列出所有待辦事項
  private void handleGet(HttpExchange exchange) throws IOException {
    byte[] response = toJSON(todosRepository.getTodos()).getBytes(StandardCharsets.UTF_8);

    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "application/json; charset=UTF-8");

    exchange.sendResponseHeaders(200, response.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response);
    }
  }

  // POST，新增待辦事項
  private void handlePost(HttpExchange exchange) throws IOException {
    try (InputStream input = exchange.getRequestBody()) {
      Optional<TodoEntity> createdTodo = todosRepository
          .createTodo(fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (createdTodo.isPresent()) {
        responseBytes = ("已新增待辦事項: " + toJSON(createdTodo.get())).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(201, responseBytes.length);
      } else {
        responseBytes = "新增失敗".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = "未發現 Todo item".getBytes(StandardCharsets.UTF_8);
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
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "缺少 ID 參數或格式錯誤".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    if (!todosRepository.getTodoById(todoId).isPresent()) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "缺少 ID 參數 or 找不到該筆待辦事項".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    try (InputStream input = exchange.getRequestBody()) {
      Optional<TodoEntity> updatedTodo = todosRepository.updateTodo(todoId,
          fromJSON(new String(input.readAllBytes(), StandardCharsets.UTF_8), TodoEntity.class));

      byte[] responseBytes;
      if (updatedTodo.isPresent()) {
        responseBytes = ("已更新待辦事項: " + toJSON(updatedTodo.get())).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
      } else {
        responseBytes = ("更新 Todo failed with id -> " + todoId).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
      }

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = "未發現 Todo item".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }

  // DELETE，刪除指定的待辦事項
  private void handleDelete(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    int todoId;
    try {
      todoId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "缺少 ID 參數或格式錯誤".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    try (OutputStream os = exchange.getResponseBody()) {
      Optional<TodoEntity> deletedTodo = todosRepository.deleteTodo(todoId);

      byte[] responseBytes;
      if (deletedTodo.isPresent()) {
        responseBytes = ("已刪除待辦事項: " + toJSON(deletedTodo.get())).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
      } else {
        responseBytes = ("找不到該筆待辦事項").getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, responseBytes.length);
      }

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
