package todolist.utils.database.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import todolist.utils.loader.ConfigLoader;

// https://redis.io/docs/latest/develop/connect/clients/java/lettuce
public class RedisManagerImpl implements RedisManager {
  // volatile keyword ensures visibility of changes to instance
  private static volatile RedisManager instance;
  private final RedisClient redisClient;
  private final StatefulRedisConnection<String, String> connection;
  private final RedisCommands<String, String> syncCommands;
  private final RedisAsyncCommands<String, String> asyncCommands;

  // Resource management using DefaultClientResources
  private static final DefaultClientResources clientResources = DefaultClientResources.builder().build();

  private RedisManagerImpl() {
    redisClient = RedisClient.create(clientResources, ConfigLoader.load(RedisManagerImpl.class, "redis.url"));

    // Setting up Redis connection options
    redisClient.setOptions(ClientOptions.builder().autoReconnect(true).build());

    // Create connection and commands
    connection = redisClient.connect();
    syncCommands = connection.sync();
    asyncCommands = connection.async();

    // Clean up resources on JVM shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  public static RedisManager getInstance() {
    if (instance == null) { // First check (no locking)
      synchronized (RedisManagerImpl.class) {
        if (instance == null) { // Second check (with locking)
          instance = new RedisManagerImpl();
        }
      }
    }
    return instance;
  }

  // String operations
  @Override
  public String get(String key) {
    return syncCommands.get(key);
  }

  @Override
  public void set(String key, String value) {
    syncCommands.set(key, value);
  }

  @Override
  public void setWithExpiry(String key, String value, long seconds) {
    syncCommands.setex(key, seconds, value);
  }

  @Override
  public Long increment(String key) {
    return syncCommands.incr(key);
  }

  @Override
  public void delete(String key) {
    syncCommands.del(key);
  }

  // List operations
  @Override
  public void lpush(String key, String... values) {
    syncCommands.lpush(key, values);
  }

  @Override
  public List<String> lrange(String key, long start, long stop) {
    return syncCommands.lrange(key, start, stop);
  }

  @Override
  public String lpop(String key) {
    return syncCommands.lpop(key);
  }

  // Set operations
  @Override
  public void sadd(String key, String... members) {
    syncCommands.sadd(key, members);
  }

  @Override
  public Set<String> smembers(String key) {
    return syncCommands.smembers(key);
  }

  @Override
  public boolean sismember(String key, String member) {
    return syncCommands.sismember(key, member);
  }

  // Hash operations
  @Override
  public void hset(String key, String field, String value) {
    syncCommands.hset(key, field, value);
  }

  @Override
  public String hget(String key, String field) {
    return syncCommands.hget(key, field);
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    return syncCommands.hgetall(key);
  }

  // Sorted set operations
  @Override
  public void zadd(String key, double score, String member) {
    syncCommands.zadd(key, score, member);
  }

  @Override
  public List<String> zrange(String key, long start, long stop) {
    return syncCommands.zrange(key, start, stop);
  }

  // Asynchronous operations
  @Override
  public CompletableFuture<String> getAsync(String key) {
    return asyncCommands.get(key).toCompletableFuture();
  }

  @Override
  public CompletableFuture<Void> setAsync(String key, String value) {
    return asyncCommands.set(key, value).toCompletableFuture().thenApply(v -> null);
  }

  @Override
  public CompletableFuture<String> getAsyncWithTimeout(String key, long timeout, TimeUnit unit) {
    return getAsync(key).orTimeout(timeout, unit);
  }

  // Shut down resources
  @Override
  public void shutdown() {
    connection.close();
    redisClient.shutdown();
    clientResources.shutdown();
  }
}
