package todolist.utils.logger;

public interface Logger {
  void info(String message);

  void warn(String message);

  void error(String message);

  void debug(String message);
}
