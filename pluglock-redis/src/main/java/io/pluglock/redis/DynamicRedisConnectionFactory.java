package io.pluglock.redis;

import io.pluglock.redis.spi.JedisConnectionFactoryImpl;
import io.pluglock.redis.spi.LettuceConnectionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态Redis连接工厂，根据类路径中的库自动选择合适的连接工厂实现
 */
public class DynamicRedisConnectionFactory implements RedisConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRedisConnectionFactory.class);
    
    private final RedisConnectionFactory delegate;
    
    public DynamicRedisConnectionFactory() {
        this.delegate = createDelegate();
    }
    
    private RedisConnectionFactory createDelegate() {
        // 优先检查Jedis
        if (JedisConnectionFactoryImpl.isSupported()) {
            logger.info("Jedis library detected, using JedisConnectionFactoryImpl");
            return new JedisConnectionFactoryImpl();
        }
        
        // 然后检查Lettuce
        if (LettuceConnectionFactoryImpl.isSupported()) {
            logger.info("Lettuce library detected, using LettuceConnectionFactoryImpl");
            return new LettuceConnectionFactoryImpl();
        }
        
        throw new IllegalStateException("No supported Redis client library found in classpath. " +
                "Please add either Jedis or Lettuce dependency to your project.");
    }
    
    @Override
    public <T> RedisConnection<T> getConnection() {
        return delegate.getConnection();
    }
    
    @Override
    public void releaseConnection(RedisConnection<?> connection) {
        delegate.releaseConnection(connection);
    }
    
    @Override
    public void destroy() {
        delegate.destroy();
    }
    
    @Override
    public String getName() {
        return "dynamic-" + delegate.getName();
    }
    
    /**
     * 获取实际使用的连接工厂实现
     * @return 实际的连接工厂
     */
    public RedisConnectionFactory getDelegate() {
        return delegate;
    }
}