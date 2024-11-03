package todolist;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodosHandler implements HttpHandler {

  private List<String> todoList = new ArrayList<>();

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
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

  // unused actually just for see binary of string
  static void printBinary(String str) {
    System.out.println("original string -> " + str);
    StringBuilder binaryString = new StringBuilder();
    for (byte b : str.getBytes()) {
      binaryString.append(String.format("%8s", Integer.toBinaryString(b &
          0xFF)).replace(' ', '0'));
    }
    System.out.println("binary string -> " + binaryString.toString());
  }

  private <T> String toJSON(T values) {
    Gson gson = new Gson();
    return gson.toJson(values);
  }

  // GET，列出所有待辦事項
  private void handleGet(HttpExchange exchange) throws IOException {
    byte[] response = toJSON(todoList).getBytes(StandardCharsets.UTF_8);

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
      String requestBody = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      todoList.add(requestBody);
      String response = "已新增待辦事項: " + requestBody;

      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(201, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }

  // PATCH，更新待辦事項
  private void handlePatch(HttpExchange exchange) throws IOException {
    if (todoList.isEmpty()) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "無待辦事項".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

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

    if (todoId < 0 || todoList.get(todoId) == null) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "缺少 ID 參數 or 找不到該筆待辦事項".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    try (InputStream input = exchange.getRequestBody()) {
      String requestBody = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      todoList.set(todoId, requestBody);
      String response = "已更新待辦事項: " + requestBody;

      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, responseBytes.length); // Use 200 for a successful update
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }

  // DELETE，刪除指定的待辦事項
  private void handleDelete(HttpExchange exchange) throws IOException {
    if (todoList.isEmpty()) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "[]".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    String query = exchange.getRequestURI().getQuery();
    Map<String, String> params = parseQuery(query);
    // The id is actually mean the index of todoList
    String todoId = params.get("id");

    if (todoId == null) {
      try (OutputStream os = exchange.getResponseBody()) {
        byte[] responseBytes = "缺少 ID 參數".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, responseBytes.length);
        os.write(responseBytes);
      }
      return;
    }

    String response;
    try {
      int id = Integer.parseInt(todoId);
      if (id >= 0 && id < todoList.size()) {
        String removedItem = todoList.remove(id);
        response = "已刪除待辦事項: " + removedItem;
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
      } else {
        response = "無效的 ID";
        exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
      }
    } catch (NumberFormatException e) {
      response = "ID 必須是數字";
      exchange.sendResponseHeaders(400, response.getBytes(StandardCharsets.UTF_8).length);
    }

    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes(StandardCharsets.UTF_8));
    }
  }

  // URL query string parser
  private Map<String, String> parseQuery(String query) {
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
}
