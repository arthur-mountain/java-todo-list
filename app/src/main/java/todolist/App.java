package todolist;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

import todolist.controllers.TodoController;
import todolist.repositories.TodoRepositoryImpl;
import todolist.utils.database.DatabaseManagerImpl;

import todolist.controllers.TodoMongoController;
import todolist.repositories.TodoMongoRepositoryImpl;
import todolist.utils.database.MongoManagerImpl;

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

    // 建立上下文，
    // 初始化 `postgresql` manager -> 注入 DatabaseManagerImpl, 初始化 todo repository ->
    // 指定 URL 路徑和處理器 controller
    server.createContext("/v1/todos", new TodoController(new TodoRepositoryImpl(new DatabaseManagerImpl())));

    // 建立上下文，
    // 初始化 `mongo` manager -> 注入 MongoManagerImpl, 初始化 todo repository ->
    // 指定 URL 路徑和處理器 controller
    server.createContext("/v2/todos", new TodoMongoController(new TodoMongoRepositoryImpl(new MongoManagerImpl())));

    // 設置執行緒池，null 表示默認執行緒池
    server.setExecutor(null);

    // 啟動伺服器
    server.start();
    System.out.println("Server is running, listening port: " + PORT);
  }
}
