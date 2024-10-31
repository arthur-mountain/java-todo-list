package todolist;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TodoListHttpServer {
  private static int PORT = 8080;
  private static List<String> todoList = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    try {
      String portEnv = System.getenv("PORT");
      if (portEnv != null) {
        PORT = Integer.parseInt(portEnv);
      }
    } catch (Exception e) {
      PORT = 8080;
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

    server.createContext("/todos", new TodoHandler());

    server.setExecutor(null);
    server.start();
    System.out.println("Server is running, listening port: " + PORT);
  }

  static class TodoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String method = exchange.getRequestMethod();

      switch (method) {
        case "GET":
          System.out.println("GET: \n" + exchange);
          handleGet(exchange);
          break;
        case "POST":
          System.out.println("POST: \n" + exchange);
          handlePost(exchange);
          break;
        case "DELETE":
          System.out.println("DELETE: \n" + exchange);
          handleDelete(exchange);
          break;
        default:
          System.out.println("Method Not Allowed");
          exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
      }
    }

    // GET，列出所有待辦事項
    private void handleGet(HttpExchange exchange) throws IOException {
      StringBuilder response = new StringBuilder("[");
      for (int i = 0; i < todoList.size(); i++) {
        response.append("\"").append(todoList.get(i)).append("\"");
        if (i < todoList.size() - 1) {
          response.append(",");
        }
      }
      response.append("]");

      byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }

    // POST，新增待辦事項
    private void handlePost(HttpExchange exchange) throws IOException {
      InputStream is = exchange.getRequestBody();
      String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      todoList.add(requestBody);
      String response = "已新增待辦事項: " + requestBody;

      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(201, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }

    // DELETE，刪除指定的待辦事項
    private void handleDelete(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      Map<String, String> params = parseQuery(query);
      String todoId = params.get("id");

      String response;
      if (todoId != null) {
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
      } else {
        response = "缺少 ID 參數";
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
}
