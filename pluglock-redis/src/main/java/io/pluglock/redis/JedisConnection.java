package io.pluglock.redis;

import io.pluglock.core.StorageCallback;

/**
 * Jedis连接实现
 */
public class JedisConnection implements RedisConnection<redis.clients.jedis.Jedis> {
    
    private final redis.clients.jedis.Jedis jedis;
    
    public JedisConnection(redis.clients.jedis.Jedis jedis) {
        this.jedis = jedis;
    }
    
    @Override
    public <R> R execute(StorageCallback<redis.clients.jedis.Jedis, R> callback) {
        return callback.doInStorage(jedis);
    }
    
    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
    
    @Override
    public redis.clients.jedis.Jedis getNativeConnection() {
        return jedis;
    }
}