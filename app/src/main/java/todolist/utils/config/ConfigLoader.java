package todolist.utils.config;

import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public class ConfigLoader {
  public static Map<String, String> load(Class<?> cls, String[] keys) {
    try (InputStream input = cls.getClassLoader().getResourceAsStream("db.properties")) {
      Properties properties = new Properties();

      if (input == null) {
        System.out.println("Sorry, unable to find db.properties");
        throw new RuntimeException("Database configuration is not set.");
      }

      properties.load(input);

      Map<String, String> config = new HashMap<>();

      for (String key : keys) {
        String value = properties.getProperty(key);
        if (value == null) {
          throw new RuntimeException("Database configuration is not fully set.");
        }
        config.put(key, value);
      }

      return config;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error loading configuration.", ex);
    }
  }
}
