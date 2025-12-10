package io.pluglock.redis.spi;

import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.LettuceConnection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lettuce连接工厂SPI实现
 */
public class LettuceConnectionFactoryImpl implements RedisConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(LettuceConnectionFactoryImpl.class);
    
    private final RedisClient redisClient;
    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    
    public LettuceConnectionFactoryImpl() {
        // 检查Lettuce类是否存在
        try {
            Class.forName("io.lettuce.core.RedisClient");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Lettuce library not found in classpath", e);
        }
        
        this("localhost", 6379);
    }
    
    public LettuceConnectionFactoryImpl(String host, int port) {
        // 检查Lettuce类是否存在
        try {
            Class.forName("io.lettuce.core.RedisClient");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Lettuce library not found in classpath", e);
        }
        
        String redisUri = "redis://" + host + ":" + port;
        this.redisClient = RedisClient.create(redisUri);
        
        // 创建连接池配置
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        
        // 创建连接池
        this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(
            () -> redisClient.connect(), poolConfig);
    }
    
    @Override
    public RedisConnection<StatefulRedisConnection<String, String>> getConnection() {
        try {
            if (connectionPool == null) {
                throw new IllegalStateException("Connection pool not initialized");
            }
            return new LettuceConnection(connectionPool.borrowObject());
        } catch (Exception e) {
            throw new RuntimeException("Failed to borrow connection from pool", e);
        }
    }
    
    @Override
    public void releaseConnection(RedisConnection connection) {
        if (connection instanceof LettuceConnection) {
            try {
                connectionPool.returnObject(((LettuceConnection) connection).getNativeConnection());
            } catch (Exception e) {
                // 如果归还连接失败，则销毁该连接
                connection.close();
            }
        }
    }
    
    @Override
    public void destroy() {
        if (connectionPool != null) {
            connectionPool.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
    
    @Override
    public String getName() {
        return "lettuce";
    }
    
    /**
     * 检查当前环境是否支持Lettuce
     * @return 是否支持Lettuce
     */
    public static boolean isSupported() {
        try {
            Class.forName("io.lettuce.core.RedisClient");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}