package io.pluglock.redis.spi;

import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.JedisConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis连接工厂SPI实现
 */
public class JedisConnectionFactoryImpl implements RedisConnectionFactory {
    
    private JedisPool jedisPool;
    
    public JedisConnectionFactoryImpl() {
        // 默认配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379, 2000);
    }
    
    public JedisConnectionFactoryImpl(JedisPoolConfig poolConfig, String host, int port, int timeout) {
        this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
    }
    
    @Override
    public RedisConnection<Jedis> getConnection() {
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
}