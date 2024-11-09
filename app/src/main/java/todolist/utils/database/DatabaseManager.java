package todolist.utils.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseManager {
  DatabaseConnection getConnection() throws SQLException;

  void releaseConnection(Connection connection);

  void shutdown();

  int getPoolSize();
}
