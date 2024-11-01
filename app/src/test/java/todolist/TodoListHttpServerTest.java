package todolist;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TodoListHttpServerTest {

  private static int PORT = 8080;
  private static HttpServer server;

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
  public void testGetEmptyTodos() throws Exception {
    URL url = createUrl("http://localhost:8080/todos");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    assertEquals(200, connection.getResponseCode());
    assertEquals("[]", readResponse(connection));
  }

  @Test
  public String testPostTodo() throws Exception {
    URL url = createUrl("http://localhost:8080/todos");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);

    String newTodo = "{\"name\": \"test\"}";
    try (OutputStream os = connection.getOutputStream()) {
      os.write(newTodo.getBytes(StandardCharsets.UTF_8));
    }

    assertEquals(201, connection.getResponseCode());
    assertEquals("已新增待辦事項: " + newTodo, readResponse(connection));
    return newTodo;
  }

  @Test
  public void testGetTodosWithOneItem() throws Exception {
    // First, add an item
    String createdTodo = testPostTodo();

    // Now, retrieve it with GET
    URL url = createUrl("http://localhost:8080/todos");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    assertEquals(200, connection.getResponseCode());
    assertEquals("[\"" + createdTodo + "\"]", readResponse(connection));
  }

  // HttpURLConnection not support PATCH method, even if there has a workaround.
  // For practice purpose, we will not cover this test case.
  // @Test
  // public void testPatchTodo() throws Exception {
  // // First, add an item
  // testPostTodo();
  //
  // // Update the item with PATCH
  // URL url = createUrl("http://localhost:8080/todos/0");
  // HttpURLConnection connection = (HttpURLConnection) url.openConnection();
  // connection.setRequestMethod("PATCH");
  // connection.setDoOutput(true);
  //
  // String updatedTodo = "{\"name\": \"example\"}";
  // try (OutputStream os = connection.getOutputStream()) {
  // os.write(updatedTodo.getBytes(StandardCharsets.UTF_8));
  // }
  //
  // assertEquals(200, connection.getResponseCode());
  // assertEquals("已更新待辦事項: " + updatedTodo, readResponse(connection));
  //
  // // Verify update with GET
  // URL getUrl = createUrl("http://localhost:8080/todos");
  // HttpURLConnection getConnection = (HttpURLConnection)
  // getUrl.openConnection();
  // getConnection.setRequestMethod("GET");
  //
  // assertEquals(200, getConnection.getResponseCode());
  // assertEquals(updatedTodo, readResponse(getConnection));
  // }

  @Test
  public void testDeleteTodo() throws Exception {
    // First, add an item
    String createdTodo = testPostTodo();

    // Delete the item
    URL url = createUrl("http://localhost:8080/todos?id=0");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("DELETE");

    assertEquals(200, connection.getResponseCode());
    assertEquals("已刪除待辦事項: " + createdTodo, readResponse(connection));

    // Verify deletion with GET
    URL getUrl = createUrl("http://localhost:8080/todos");
    HttpURLConnection getConnection = (HttpURLConnection) getUrl.openConnection();
    getConnection.setRequestMethod("GET");

    assertEquals(200, getConnection.getResponseCode());
    assertEquals("[]", readResponse(getConnection));
  }
}
