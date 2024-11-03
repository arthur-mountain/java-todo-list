package todolist;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import todolist.controllers.TodoController;
import todolist.entitys.TodoEntity;
import todolist.repositories.TodoRepository;

public class TodoListHttpServerTest {

  private static int PORT = 8080;
  private static HttpServer server;
  private static final TodoRepository todosRepository = new TodoRepository();

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
    server.createContext("/todos", new TodoController());
    server.setExecutor(null); // creates a default executor
    server.start();
    Thread.sleep(300); // Adjust the delay if needed
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
  private final HttpURLConnection _createConnection(String url, String method) {
    try {
      URL uri = URI.create(url).toURL();
      HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
      connection.setRequestMethod(method);
      connection.setRequestProperty("Content-Type", "application/json");
      return connection;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private HttpURLConnection createConnection(String url) {
    return _createConnection(url, "GET");
  }

  private HttpURLConnection createConnection(String url, String method) {
    return _createConnection(url, method);
  }

  @Test
  public void testGetTodos() throws Exception {
    HttpURLConnection connection = createConnection("http://localhost:8080/todos");
    assertEquals(200, connection.getResponseCode());
    assertEquals(new Gson().toJson(todosRepository.getTodos()), readResponse(connection));
  }

  @Test
  public void testPostTodo() throws Exception {
    HttpURLConnection connection = createConnection("http://localhost:8080/todos", "POST");
    connection.setDoOutput(true);

    String newTodo = new Gson().toJson(new HashMap<>(Map.of("title", "test")));
    try (OutputStream os = connection.getOutputStream()) {
      os.write(newTodo.getBytes(StandardCharsets.UTF_8));
    }

    assertEquals(201, connection.getResponseCode());
    assertEquals("test",
        new Gson().fromJson(readResponse(connection), TodoEntity.class).title);
  }

  @Test
  @Disabled("HttpServer does not support PUT method")
  public void testPatchTodo() throws Exception {
  }

  @Test
  public void testDeleteTodo() throws Exception {
    List<TodoEntity> todos = todosRepository.getTodos();
    TodoEntity lastTodo = todos.get(todos.size() - 1);
    HttpURLConnection connection = createConnection("http://localhost:8080/todos/" + lastTodo.id, "DELETE");

    assertEquals(200, connection.getResponseCode());

    todos = todosRepository.getTodos();
    assertFalse(todos.stream().anyMatch(todo -> todo.id == lastTodo.id));
  }
}
