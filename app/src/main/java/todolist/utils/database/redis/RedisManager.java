package todolist.utils.database.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * String operations: get, set, setWithExpiry, increment, delete for basic
 * string manipulation.
 *
 * List operations: lpush, lrange, and lpop for handling Redis lists.
 *
 * Set operations: sadd, smembers, and sismember for Redis sets.
 *
 * Hash operations: hset, hget, and hgetAll for Redis hash maps.
 *
 * Sorted set operations: zadd and zrange for ordered data using sorted sets.
 *
 * Asynchronous operations: getAsync, setAsync, and getAsyncWithTimeout for
 * non-blocking operations.
 *
 * Resource management: shutdown to release connections and resources on JVM
 * shutdown.
 */
public interface RedisManager {

  // String operations
  String get(String key);

  void set(String key, String value);

  void setWithExpiry(String key, String value, long seconds);

  Long increment(String key);

  void delete(String key);

  // List operations
  void lpush(String key, String... values);

  List<String> lrange(String key, long start, long stop);

  String lpop(String key);

  // Set operations
  void sadd(String key, String... members);

  Set<String> smembers(String key);

  boolean sismember(String key, String member);

  // Hashtable operations
  void hset(String key, String field, String value);

  String hget(String key, String field);

  Map<String, String> hgetAll(String key);

  // Sorted Set operations
  void zadd(String key, double score, String member);

  List<String> zrange(String key, long start, long stop);

  // Asynchronous operations
  CompletableFuture<String> getAsync(String key);

  CompletableFuture<Void> setAsync(String key, String value);

  CompletableFuture<String> getAsyncWithTimeout(String key, long timeout, TimeUnit unit);

  // Resource management
  void shutdown();
}
