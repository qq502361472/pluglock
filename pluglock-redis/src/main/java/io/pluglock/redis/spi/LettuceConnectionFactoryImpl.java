package io.pluglock.redis.spi;

import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.LettuceConnection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Lettuce连接工厂SPI实现
 */
public class LettuceConnectionFactoryImpl implements RedisConnectionFactory {
    
    private final RedisClient redisClient;
    
    public LettuceConnectionFactoryImpl() {
        this("localhost", 6379);
    }
    
    public LettuceConnectionFactoryImpl(String host, int port) {
        String redisUri = "redis://" + host + ":" + port;
        this.redisClient = RedisClient.create(redisUri);
    }
    
    @Override
    public RedisConnection<StatefulRedisConnection<String, String>> getConnection() {
        return new LettuceConnection(redisClient.connect());
    }
    
    @Override
    public void releaseConnection(RedisConnection connection) {
        if (connection instanceof LettuceConnection) {
            connection.close();
        }
    }
    
    @Override
    public void destroy() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
    
    @Override
    public String getName() {
        return "lettuce";
    }
}