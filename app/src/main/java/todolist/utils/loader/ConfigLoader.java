package todolist.utils.loader;

import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public class ConfigLoader {
  public static Map<String, String> load(Class<?> cls, String[] keys) {
    try (InputStream input = cls.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        throw new RuntimeException("Database configuration is not set.");
      }

      Properties properties = new Properties();
      properties.load(input);

      Map<String, String> config = new HashMap<>();
      for (String key : keys) {
        String value = properties.getProperty(key);
        if (value == null) {
          throw new RuntimeException("Config loader could not found key with name: " + key);
        }
        config.put(key, value);
      }

      return config;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error loading configuration.", ex);
    }
  }

  public static Map<String, String> load(Class<?> cls, Map<String, String> keyMap) {
    try (InputStream input = cls.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        throw new RuntimeException("Database configuration is not set.");
      }

      Properties properties = new Properties();
      properties.load(input);

      Map<String, String> config = new HashMap<>();
      for (Map.Entry<String, String> entry : keyMap.entrySet()) {
        String originalKey = entry.getKey();
        String value = properties.getProperty(originalKey);
        if (value == null) {
          throw new RuntimeException("Config loader could not found key with name: " + originalKey);
        }
        // used the mappedKey from entry.getValue()
        config.put(entry.getValue(), value);
      }

      return config;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error loading configuration.", ex);
    }
  }

  public static String load(Class<?> cls, String key) {
    try (InputStream input = cls.getClassLoader().getResourceAsStream("db.properties")) {
      if (input == null) {
        throw new RuntimeException("Database configuration is not set.");
      }

      Properties properties = new Properties();
      properties.load(input);

      return properties.getProperty(key);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error loading configuration.", ex);
    }
  }
}
