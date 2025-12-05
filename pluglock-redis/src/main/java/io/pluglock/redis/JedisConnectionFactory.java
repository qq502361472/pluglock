package io.pluglock.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis连接工厂实现
 */
public class JedisConnectionFactory implements RedisConnectionFactory {
    
    private JedisPool jedisPool;
    
    public JedisConnectionFactory(String host, int port) {
        this(new JedisPoolConfig(), host, port, 2000);
    }
    
    public JedisConnectionFactory(JedisPoolConfig poolConfig, String host, int port, int timeout) {
        this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
    }
    
    @Override
    public RedisConnection<Jedis> getConnection() {
        return new JedisConnection(jedisPool.getResource());
    }
    
    @Override
    public void releaseConnection(RedisConnection<?> connection) {
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