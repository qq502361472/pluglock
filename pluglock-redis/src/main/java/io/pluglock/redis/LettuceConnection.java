package io.pluglock.redis;

import io.pluglock.core.StorageCallback;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Lettuce连接实现
 */
public class LettuceConnection implements RedisConnection<StatefulRedisConnection<String, String>> {
    
    private final StatefulRedisConnection<String, String> connection;
    
    public LettuceConnection(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }
    
    @Override
    public <R> R execute(StorageCallback<StatefulRedisConnection<String, String>, R> callback) {
        return callback.doInStorage(connection);
    }
    
    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
    
    @Override
    public StatefulRedisConnection<String, String> getNativeConnection() {
        return connection;
    }
}