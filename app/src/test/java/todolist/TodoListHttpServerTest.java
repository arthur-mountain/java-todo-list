package todolist;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TodoListHttpServerTest {

  private static int PORT = 8080;
  private static HttpServer server;
  private static final TodosRepository todosRepository = new TodosRepository();

  @BeforeAll
  public static void setUp() throws Exception {
    try {
      String portEnv = System.getenv("PORT");
      if (portEnv != null) {
        PORT = Integer.parseInt(portEnv);
      }
    } catch (Exception e) {
      PORT = 8080;
    }
    server = HttpServer.create(new InetSocketAddress(PORT), 0);
    server.createContext("/todos", new TodosHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
    Thread.sleep(300); // Adjust the delay if needed
  }

  @BeforeEach
  public void clearTodos() throws Exception {
    URL url = createUrl("http://localhost:8080/todos?id=0");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("DELETE");
    connection.getResponseCode(); // Trigger the request
  }

  @AfterAll
  public static void tearDown() {
    server.stop(0);
  }

  // Helper method to read the response from HttpURLConnection
  private String readResponse(HttpURLConnection connection) throws Exception {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        content.append(line);
      }
      return content.toString();
    }
  }

  // Helper method to create URL from a String
  private URL createUrl(String url) {
    try {
      URI uri = URI.create(url);
      return uri.toURL();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetTodos() throws Exception {
    URL url = createUrl("http://localhost:8080/todos");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Content-Type", "application/json");

    assertEquals(200, connection.getResponseCode());
    assertEquals(new Gson().toJson(todosRepository.getTodos()), readResponse(connection));
  }
}
