package todolist.utils.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseManager {
  Connection getConnection() throws SQLException;

  void releaseConnection(Connection connection);

  void shutdown();

  int getPoolSize();
}
