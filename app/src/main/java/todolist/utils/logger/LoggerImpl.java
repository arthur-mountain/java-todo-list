package todolist.utils.logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;

public class LoggerImpl implements Logger {
  private static volatile LoggerImpl instance;
  private final String LOGS_DIRECTORY = "logs";
  private final java.util.logging.Logger logger;
  private FileHandler fileHandler;
  private Formatter logFormatter;
  private String currentLogFilename;

  private LoggerImpl(Class<?> clazz) {
    this.logger = java.util.logging.Logger.getLogger(clazz.getName());
    configureLogger();
  }

  public static LoggerImpl getInstance(Class<?> clazz) {
    if (instance == null) { // 第一次檢查
      synchronized (LoggerImpl.class) { // 加鎖
        if (instance == null) { // 第二次檢查
          instance = new LoggerImpl(clazz);
        }
      }
    }
    return instance;
  }

  private final void configureFormatter() {
    logFormatter = new SimpleFormatter() {
      @Override
      public String format(LogRecord record) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis()));
        String level = record.getLevel().getName();
        String message = formatMessage(record);

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp).append("] ");
        sb.append("[").append(level).append("] ");
        sb.append(message);

        if (record.getThrown() != null) {
          Throwable thrown = record.getThrown();
          sb.append(" ").append(thrown.getClass().getName())
              .append(" - ").append(thrown.getMessage());
        }

        return sb.append(System.lineSeparator()).toString();
      }
    };
  }

  private void configureLogger() {
    configureLogger(null);
  }

  private void configureLogger(String configPath) {
    logger.setUseParentHandlers(false); // 禁用父处理器，避免重复输出

    if (configPath != null && !configPath.isEmpty()) {
      try (InputStream configFile = LoggerImpl.class.getClassLoader().getResourceAsStream(configPath)) {
        LogManager.getLogManager().readConfiguration(configFile);
      } catch (IOException e) {
        System.out.println("Failed to load logger configuration file, using default configuration.");
      }
    }

    if (logFormatter == null) {
      configureFormatter();
    }

    // console handler
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.ALL);
    consoleHandler.setFormatter(logFormatter);
    logger.addHandler(consoleHandler);
    // file handler
    // setFileHandler();
    // scheduleDailyLogFileUpdate();
  }

  private final void setFileHandler() {
    try {
      String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      String newLogFilename = String.format("%s/todolist-%s.log", LOGS_DIRECTORY, date);

      // 检查是否需要更新 file handler
      if (fileHandler == null || !newLogFilename.equals(currentLogFilename)) {
        if (fileHandler != null) {
          logger.removeHandler(fileHandler); // remove old file handler
          fileHandler.close(); // close old file handler
        }

        // log file
        Path logDir = Paths.get(LOGS_DIRECTORY);
        if (!Files.exists(logDir)) {
          Files.createDirectories(logDir);
        }

        if (logFormatter == null) {
          configureFormatter();
        }

        // create new file handler
        fileHandler = new FileHandler(newLogFilename, true);
        fileHandler.setLevel(Level.WARNING);
        fileHandler.setFormatter(logFormatter);
        logger.addHandler(fileHandler);
        currentLogFilename = newLogFilename;
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to configure logger", e);
    }
  }

  private final void scheduleDailyLogFileUpdate() {
    Timer timer = new Timer(true);
    Date now = new Date();
    long midnightTime = (now.getTime() / (24 * 60 * 60 * 1000) + 1) * (24 * 60 * 60 * 1000);

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        setFileHandler();
      }
    }, midnightTime, 24 * 60 * 60 * 1000); // 每天執行一次
  }

  @Override
  public void info(String message) {
    logger.info(message);
  }

  @Override
  public void warn(String message) {
    logger.warning(message);
  }

  @Override
  public void error(String message) {
    logger.severe(message);
  }

  @Override
  public void debug(String message) {
    logger.fine(message);
  }
}
