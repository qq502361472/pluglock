package io.pluglock.redis;

import io.pluglock.core.AbstractPLockResource;
import io.pluglock.core.PLockEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Redis锁资源抽象基类
 */
public abstract class RedisPLockResource extends AbstractPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(RedisPLockResource.class);
    
    protected final RedisConnectionFactory connectionFactory;
    
    // 定义获取锁的Lua脚本
    protected static final String ACQUIRE_SCRIPT = 
        "if (redis.call('exists', KEYS[1]) == 0) then " +
        "redis.call('hset', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
        "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "return redis.call('pttl', KEYS[1]);";
        
    // 定义尝试获取锁的Lua脚本
    protected static final String TRY_ACQUIRE_SCRIPT =
        "if (redis.call('exists', KEYS[1]) == 0) then " +
        "redis.call('hset', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "return redis.call('pttl', KEYS[1]);";
        
    // 定义释放锁的Lua脚本
    protected static final String RELEASE_SCRIPT =
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
        "return nil;" +
        "end; " +
        "local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1); " +
        "if (counter > 0) then " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return 0; " +
        "else " +
        "redis.call('del', KEYS[1]); " +
        "redis.call('publish', KEYS[2], ARGV[3]); " +
        "return 1; " +
        "end; " +
        "return nil;";

    public RedisPLockResource() {
        this.connectionFactory = loadConnectionFactory();
    }
    
    public RedisPLockResource(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    /**
     * 通过SPI加载连接工厂
     * @return Redis连接工厂
     */
    private RedisConnectionFactory loadConnectionFactory() {
        ServiceLoader<RedisConnectionFactory> loader = ServiceLoader.load(RedisConnectionFactory.class);
        for (RedisConnectionFactory factory : loader) {
            logger.info("Loaded Redis connection factory: {}", factory.getName());
            return factory;
        }
        
        // 如果SPI加载失败，使用默认的动态连接工厂
        logger.warn("No RedisConnectionFactory found via SPI, using DynamicRedisConnectionFactory as fallback");
        return new DynamicRedisConnectionFactory();
    }
    
    @Override
    public Long acquireResource(String name, long leaseTime, TimeUnit unit, long threadId) {
        RedisConnection<?> connection = null;
        try {
            connection = connectionFactory.getConnection();
            return doAcquireResource(connection, name, leaseTime, unit, threadId);
        } catch (Exception e) {
            logger.error("Failed to acquire Redis lock: {}", name, e);
            throw new RuntimeException("Failed to acquire Redis lock", e);
        } finally {
            if (connection != null) {
                try {
                    connectionFactory.releaseConnection(connection);
                } catch (Exception e) {
                    logger.warn("Failed to release Redis connection", e);
                }
            }
        }
    }
    
    private Long doAcquireResource(RedisConnection<?> connection, String name, long leaseTime, TimeUnit unit, long threadId) {
        return executeAcquireScript(connection, name, leaseTime, unit, threadId);
    }
    
    protected Long executeAcquireScript(RedisConnection<?> connection, String name, long leaseTime, TimeUnit unit, long threadId) {
        String script = ACQUIRE_SCRIPT;
        long expireTime = unit.toMillis(leaseTime);
        String threadIdentifier = String.valueOf(threadId);
        
        return executeScript(connection, script, new String[]{name}, 
                           String.valueOf(expireTime), threadIdentifier);
    }
    
    @Override
    public PLockEntry subscribe(String name) {
        // TODO: 实现订阅逻辑
        return super.subscribe(name);
    }
    
    @Override
    public void unsubscribe(String name) {
        // TODO: 实现取消订阅逻辑
        super.unsubscribe(name);
    }
    
    @Override
    public Long tryAcquireResource(String name, long threadId) {
        RedisConnection<?> connection = null;
        try {
            connection = connectionFactory.getConnection();
            return doTryAcquireResource(connection, name, threadId);
        } catch (Exception e) {
            logger.error("Failed to try acquire Redis lock: {}", name, e);
            throw new RuntimeException("Failed to try acquire Redis lock", e);
        } finally {
            if (connection != null) {
                try {
                    connectionFactory.releaseConnection(connection);
                } catch (Exception e) {
                    logger.warn("Failed to release Redis connection", e);
                }
            }
        }
    }
    
    private Long doTryAcquireResource(RedisConnection<?> connection, String name, long threadId) {
        return executeTryAcquireScript(connection, name, threadId);
    }
    
    protected Long executeTryAcquireScript(RedisConnection<?> connection, String name, long threadId) {
        String script = TRY_ACQUIRE_SCRIPT;
        long expireTime = 30000; // 默认30秒
        String threadIdentifier = String.valueOf(threadId);
        
        return executeScript(connection, script, new String[]{name}, 
                           String.valueOf(expireTime), threadIdentifier);
    }
    
    @Override
    public void releaseResource(String name, long threadId) {
        RedisConnection<?> connection = null;
        try {
            connection = connectionFactory.getConnection();
            doReleaseResource(connection, name, threadId);
        } catch (Exception e) {
            logger.error("Failed to release Redis lock: {}", name, e);
            throw new RuntimeException("Failed to release Redis lock", e);
        } finally {
            if (connection != null) {
                try {
                    connectionFactory.releaseConnection(connection);
                } catch (Exception e) {
                    logger.warn("Failed to release Redis connection", e);
                }
            }
        }
        super.releaseResource(name, threadId);
    }
    
    private void doReleaseResource(RedisConnection<?> connection, String name, long threadId) {
        executeReleaseScript(connection, name, threadId);
    }
    
    protected void executeReleaseScript(RedisConnection<?> connection, String name, long threadId) {
        String script = RELEASE_SCRIPT;
        long expireTime = 30000; // 默认30秒
        String threadIdentifier = String.valueOf(threadId);
        String channelName = getChannelName(name);
        
        executeScript(connection, script, new String[]{name, channelName}, 
                     String.valueOf(expireTime), threadIdentifier, "1");
    }
    
    private String getChannelName(String lockName) {
        return "lock:" + lockName + ":channel";
    }
    
    /**
     * 在指定连接上执行Lua脚本的抽象方法，由具体子类实现
     * 
     * @param connection Redis连接
     * @param script Lua脚本
     * @param keys 脚本KEYS参数
     * @param args 脚本ARGV参数
     * @return 脚本执行结果
     */
    protected abstract <T> T executeScript(RedisConnection<?> connection, String script, String[] keys, String... args);
    
    @Override
    protected void startWatchDog(String name, long threadId) {
        // TODO: 实现看门狗逻辑
        logger.debug("Starting watchdog for lock: {}, threadId: {}", name, threadId);
    }
    
    /**
     * 获取连接工厂
     * @return Redis连接工厂
     */
    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}