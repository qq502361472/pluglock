package io.pluglock.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于Jedis的Redis锁资源实现
 */
public class JedisPLockResource extends RedisPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(JedisPLockResource.class);
    
    public JedisPLockResource() {
        super();
    }
    
    public JedisPLockResource(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
    
    @Override
    protected <T> T executeScript(RedisConnection<?> connection, String script, String[] keys, String... args) {
        if (!(connection instanceof JedisConnection)) {
            throw new IllegalArgumentException("Connection must be an instance of JedisConnection");
        }
        
        JedisConnection jedisConnection = (JedisConnection) connection;
        redis.clients.jedis.Jedis jedis = jedisConnection.getNativeConnection();
        
        try {
            // Jedis的eval方法接受keys和args参数
            Object result = jedis.eval(script, keys.length, mergeArrays(keys, args));
            return (T) result;
        } catch (Exception e) {
            logger.error("Failed to execute script with Jedis", e);
            throw new RuntimeException("Failed to execute script with Jedis", e);
        }
    }
    
    private String[] mergeArrays(String[] keys, String[] args) {
        String[] result = new String[keys.length + args.length];
        System.arraycopy(keys, 0, result, 0, keys.length);
        System.arraycopy(args, 0, result, keys.length, args.length);
        return result;
    }
}