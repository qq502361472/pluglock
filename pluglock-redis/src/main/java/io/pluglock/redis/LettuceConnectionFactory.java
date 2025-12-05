package io.pluglock.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Lettuce连接工厂实现
 */
public class LettuceConnectionFactory implements RedisConnectionFactory {
    
    private final RedisClient redisClient;
    private final String redisUri;
    
    public LettuceConnectionFactory(String host, int port) {
        this.redisUri = "redis://" + host + ":" + port;
        this.redisClient = RedisClient.create(this.redisUri);
    }
    
    @Override
    public RedisConnection<StatefulRedisConnection<String, String>> getConnection() {
        return new LettuceConnection(redisClient.connect());
    }
    
    @Override
    public void releaseConnection(RedisConnection<?> connection) {
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