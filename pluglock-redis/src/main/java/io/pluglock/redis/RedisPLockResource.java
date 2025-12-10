package io.pluglock.redis;

import io.pluglock.core.AbstractPLockResource;
import io.pluglock.core.PLockEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Redis锁资源实现
 */
public class RedisPLockResource extends AbstractPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(RedisPLockResource.class);
    
    private final RedisConnectionFactory connectionFactory;
    
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
        // 根据连接类型调用相应的实现
        if (connection instanceof JedisConnection) {
            return doAcquireResourceWithJedis((JedisConnection) connection, name, leaseTime, unit, threadId);
        } else if (connection instanceof LettuceConnection) {
            return doAcquireResourceWithLettuce((LettuceConnection) connection, name, leaseTime, unit, threadId);
        } else {
            throw new UnsupportedOperationException("Unsupported Redis connection type: " + connection.getClass());
        }
    }
    
    private Long doAcquireResourceWithJedis(JedisConnection connection, String name, long leaseTime, TimeUnit unit, long threadId) {
        // 使用Jedis实现获取锁的逻辑
        redis.clients.jedis.Jedis jedis = connection.getNativeConnection();
        String script = "if (redis.call('exists', KEYS[1]) == 0) then " +
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
        
        try {
            long ttl = (Long) jedis.eval(script, 1, name, String.valueOf(unit.toMillis(leaseTime)), String.valueOf(threadId));
            return ttl == 0 ? null : ttl;
        } catch (Exception e) {
            logger.error("Failed to acquire lock with Jedis: {}", name, e);
            throw new RuntimeException("Failed to acquire lock with Jedis", e);
        }
    }
    
    private Long doAcquireResourceWithLettuce(LettuceConnection connection, String name, long leaseTime, TimeUnit unit, long threadId) {
        // 使用Lettuce实现获取锁的逻辑
        io.lettuce.core.api.StatefulRedisConnection<String, String> lettuceConnection = connection.getNativeConnection();
        io.lettuce.core.api.sync.RedisCommands<String, String> commands = lettuceConnection.sync();
        
        String script = "if (redis.call('exists', KEYS[1]) == 0) then " +
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
        
        try {
            Object result = commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER, new String[]{name}, 
                               String.valueOf(unit.toMillis(leaseTime)), String.valueOf(threadId));
            Long ttl = (Long) result;
            return ttl == 0 ? null : ttl;
        } catch (Exception e) {
            logger.error("Failed to acquire lock with Lettuce: {}", name, e);
            throw new RuntimeException("Failed to acquire lock with Lettuce", e);
        }
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
        // 根据连接类型调用相应的实现
        if (connection instanceof JedisConnection) {
            return doTryAcquireResourceWithJedis((JedisConnection) connection, name, threadId);
        } else if (connection instanceof LettuceConnection) {
            return doTryAcquireResourceWithLettuce((LettuceConnection) connection, name, threadId);
        } else {
            throw new UnsupportedOperationException("Unsupported Redis connection type: " + connection.getClass());
        }
    }
    
    private Long doTryAcquireResourceWithJedis(JedisConnection connection, String name, long threadId) {
        // 使用Jedis实现尝试获取锁的逻辑
        redis.clients.jedis.Jedis jedis = connection.getNativeConnection();
        String script = "if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "return redis.call('pttl', KEYS[1]);";
        
        try {
            long ttl = (Long) jedis.eval(script, 1, name, String.valueOf(30000), String.valueOf(threadId));
            return ttl == 0 ? null : ttl;
        } catch (Exception e) {
            logger.error("Failed to try acquire lock with Jedis: {}", name, e);
            throw new RuntimeException("Failed to try acquire lock with Jedis", e);
        }
    }
    
    private Long doTryAcquireResourceWithLettuce(LettuceConnection connection, String name, long threadId) {
        // 使用Lettuce实现尝试获取锁的逻辑
        io.lettuce.core.api.StatefulRedisConnection<String, String> lettuceConnection = connection.getNativeConnection();
        io.lettuce.core.api.sync.RedisCommands<String, String> commands = lettuceConnection.sync();
        
        String script = "if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "return redis.call('pttl', KEYS[1]);";
        
        try {
            Object result = commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER, new String[]{name}, 
                               String.valueOf(30000), String.valueOf(threadId));
            Long ttl = (Long) result;
            return ttl == 0 ? null : ttl;
        } catch (Exception e) {
            logger.error("Failed to try acquire lock with Lettuce: {}", name, e);
            throw new RuntimeException("Failed to try acquire lock with Lettuce", e);
        }
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
        // 根据连接类型调用相应的实现
        if (connection instanceof JedisConnection) {
            doReleaseResourceWithJedis((JedisConnection) connection, name, threadId);
        } else if (connection instanceof LettuceConnection) {
            doReleaseResourceWithLettuce((LettuceConnection) connection, name, threadId);
        } else {
            throw new UnsupportedOperationException("Unsupported Redis connection type: " + connection.getClass());
        }
    }
    
    private void doReleaseResourceWithJedis(JedisConnection connection, String name, long threadId) {
        // 使用Jedis实现释放锁的逻辑
        redis.clients.jedis.Jedis jedis = connection.getNativeConnection();
        String script = "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
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
        
        try {
            jedis.eval(script, 2, name, getChannelName(name), String.valueOf(30000), String.valueOf(threadId), "1");
        } catch (Exception e) {
            logger.error("Failed to release lock with Jedis: {}", name, e);
            throw new RuntimeException("Failed to release lock with Jedis", e);
        }
    }
    
    private void doReleaseResourceWithLettuce(LettuceConnection connection, String name, long threadId) {
        // 使用Lettuce实现释放锁的逻辑
        io.lettuce.core.api.StatefulRedisConnection<String, String> lettuceConnection = connection.getNativeConnection();
        io.lettuce.core.api.sync.RedisCommands<String, String> commands = lettuceConnection.sync();
        
        String script = "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
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
        
        try {
            commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER, new String[]{name, getChannelName(name)}, 
                       String.valueOf(30000), String.valueOf(threadId), "1");
        } catch (Exception e) {
            logger.error("Failed to release lock with Lettuce: {}", name, e);
            throw new RuntimeException("Failed to release lock with Lettuce", e);
        }
    }
    
    private String getChannelName(String lockName) {
        return "lock:" + lockName + ":channel";
    }
    
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