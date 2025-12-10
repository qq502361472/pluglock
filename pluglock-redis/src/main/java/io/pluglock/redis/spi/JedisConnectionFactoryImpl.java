package io.pluglock.redis.spi;

import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.JedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis连接工厂SPI实现
 */
public class JedisConnectionFactoryImpl implements RedisConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(JedisConnectionFactoryImpl.class);
    
    private JedisPool jedisPool;
    
    public JedisConnectionFactoryImpl() {
        // 检查Jedis类是否存在
        try {
            Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Jedis library not found in classpath", e);
        }
        
        // 默认配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWaitMillis(2000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379, 2000);
    }
    
    public JedisConnectionFactoryImpl(JedisPoolConfig poolConfig, String host, int port, int timeout) {
        // 检查Jedis类是否存在
        try {
            Class.forName("redis.clients.jedis.Jedis");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Jedis library not found in classpath", e);
        }
        
        this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
    }
    
    @Override
    public RedisConnection<Jedis> getConnection() {
        if (jedisPool == null) {
            throw new IllegalStateException("JedisPool not initialized");
        }
        return new JedisConnection(jedisPool.getResource());
    }
    
    @Override
    public void releaseConnection(RedisConnection connection) {
        if (connection instanceof JedisConnection) {
            connection.close();
        }
    }
    
    @Override
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
    
    @Override
    public String getName() {
        return "jedis";
    }
    
    /**
     * 检查当前环境是否支持Jedis
     * @return 是否支持Jedis
     */
    public static boolean isSupported() {
        try {
            Class.forName("redis.clients.jedis.Jedis");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}