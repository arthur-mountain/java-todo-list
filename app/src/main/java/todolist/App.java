package todolist;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import todolist.utils.logger.LoggerImpl;
import todolist.utils.database.DatabaseManagerImpl;
import todolist.controllers.TodoController;
import todolist.repositories.postgresql.TodoRepositoryImpl;
import todolist.repositories.postgresql.TodoRepositoryWithRedisImpl;

import todolist.utils.database.MongoManagerImpl;
import todolist.controllers.TodoMongoController;
import todolist.repositories.mongodb.TodoMongoRepositoryImpl;

public class App {
  public static void main(String[] args) throws IOException {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      LoggerImpl.getInstance(App.class)
          .error("Unhandled exception in thread:" + thread.getName() + "message:" + throwable.getMessage());
    });

    int PORT = 8080;

    try {
      String portEnv = System.getenv("PORT");
      if (portEnv != null) {
        PORT = Integer.parseInt(portEnv);
      }
    } catch (Exception e) {
      PORT = 8080;
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

    // 建立上下文，初始化 `postgresql` manager ->
    // 注入 DatabaseManagerImpl, 初始化 todo postgresql repository ->
    // 指定 URL 路徑和處理器 controller
    server.createContext("/v1/todos", new TodoController(new TodoRepositoryImpl(new DatabaseManagerImpl())));

    // 建立上下文，初始化 `mongo` manager ->
    // 注入 MongoManagerImpl, 初始化 todo mono repository ->
    // 指定 URL 路徑和處理器 controller
    server.createContext("/v2/todos", new TodoMongoController(new TodoMongoRepositoryImpl(new MongoManagerImpl())));

    // 建立上下文，初始化 `postgresql` manager ->
    // 注入 MongoManagerImpl, 初始化 todo postgresql with redis repository ->
    // 指定 URL 路徑和處理器 controller
    server.createContext("/v3/todos", new TodoController(new TodoRepositoryWithRedisImpl(new DatabaseManagerImpl())));
    // 指定 URL 路徑和處理器 controller with redis
    server.createContext("/v3/todos", new TodoController(new TodoRepositoryWithRedisImpl(new DatabaseManagerImpl())));

    // 設置執行緒池，null 表示默認執行緒池
    server.setExecutor(null);

    // 啟動伺服器
    server.start();
    System.out.println("Server is running, listening port: " + PORT);
  }
}
