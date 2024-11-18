package todolist.controllers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import todolist.utils.json.Json;
import todolist.entities.TodoKafkaEntity;
import todolist.utils.database.kafka.*;

public class TodoNotificationController implements HttpHandler {
  private final KafkaConsumerManager consumerManager = new KafkaConsumerManagerImpl();
  private final String topic = "notification";

  public TodoNotificationController() {
    // Start consumer thread
    consumerManager.start(topic);
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
        case "POST":
          System.out.println("POST: \n" + exchange);
          handlePost(exchange);
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

  // POST，新增 notification
  private void handlePost(HttpExchange exchange) throws IOException {
    try (InputStream input = exchange.getRequestBody()) {
      String data = new String(input.readAllBytes(), StandardCharsets.UTF_8);

      KafkaProducerManagerImpl.getInstance().sendMessage(topic, Json.fromJSON(data, TodoKafkaEntity.class).id, data);

      byte[] responseBytes = Json.toBytes(Json.toJSON("ok"));
      exchange.sendResponseHeaders(201, responseBytes.length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    } catch (Exception e) {
      e.printStackTrace();
      byte[] responseBytes = Json.toBytes(Json.toJSON(new HashMap<>(Map.of("message", "Missing todo item"))));
      exchange.sendResponseHeaders(400, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    }
  }
}
