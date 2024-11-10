package todolist.utils.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json {
  // volatile keyword ensures visibility of changes to instance
  private static volatile Gson instance;

  public static <T> String toJSON(T values) {
    return getInstance().toJson(values);
  }

  public static <T> T fromJSON(String json, Class<T> classOfT) {
    return fromJSONWithType(json, classOfT, null);
  }

  public static <T> T fromJSON(String json, Class<T> classOfT, T defaultValue) {
    return fromJSONWithType(json, classOfT, defaultValue);
  }

  public static <T> List<T> fromJSONToList(String json, Class<T> classOfT) {
    return fromJSONWithType(json, TypeToken.getParameterized(List.class, classOfT).getType(), new ArrayList<>());
  }

  public static <T> List<T> fromJSONToList(String json, Class<T> classOfT, List<T> defaultValue) {
    return fromJSONWithType(json, TypeToken.getParameterized(List.class, classOfT).getType(), defaultValue);
  }

  public static <K, V> Map<K, V> fromJSONToMap(String json, Class<K> keyClass, Class<V> valueClass) {
    return fromJSONWithType(json, TypeToken.getParameterized(Map.class, keyClass, valueClass).getType(),
        new HashMap<>());
  }

  public static <K, V> Map<K, V> fromJSONToMap(String json, Class<K> keyClass, Class<V> valueClass,
      Map<K, V> defaultValue) {
    return fromJSONWithType(json, TypeToken.getParameterized(Map.class, keyClass, valueClass).getType(), defaultValue);
  }

  private static Gson getInstance() {
    if (instance == null) {
      synchronized (Json.class) {
        if (instance == null) {
          instance = new Gson();
        }
      }
    }
    return instance;
  }

  private static <T> T fromJSONWithType(String json, Type typeOfT, T defaultValue) {
    try {
      return getInstance().fromJson(json, typeOfT);
    } catch (Exception e) {
      System.err.println("Deserialization failed: " + e.getMessage());
      return defaultValue;
    }
  }

  private Json() {
  }
}
