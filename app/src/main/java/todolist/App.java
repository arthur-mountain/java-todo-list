package todolist;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import todolist.controllers.TodoController;
import todolist.repositories.TodoRepositoryImpl;
import todolist.repositories.TodoRepository;
import todolist.utils.database.DatabaseManagerImpl;
import todolist.utils.database.DatabaseManager;

public class App {
  private static int PORT = 8080;

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

    // 初始化 database manager
    DatabaseManager databaseManager = new DatabaseManagerImpl();

    // 初始化 todo repository，並將 DatabaseManagerImpl 注入
    TodoRepository todoRepository = new TodoRepositoryImpl(databaseManager);

    // 建立上下文，指定 URL 路徑和處理器
    server.createContext("/todos", new TodoController(todoRepository));

    // 設置執行緒池，null 表示默認執行緒池
    server.setExecutor(null);

    // 啟動伺服器
    server.start();
    System.out.println("Server is running, listening port: " + PORT);
  }
}
