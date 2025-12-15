package io.pluglock.redis.spi;

import io.pluglock.redis.DynamicRedisConnectionFactory;
import io.pluglock.redis.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * 连接工厂加载器
 * 通过SPI机制加载Redis连接工厂实现
 */
public class ConnectionFactoryLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionFactoryLoader.class);
    
    /**
     * 通过SPI加载连接工厂
     * 
     * @return Redis连接工厂
     */
    public static RedisConnectionFactory loadConnectionFactory() {
        ServiceLoader<RedisConnectionFactory> loader = ServiceLoader.load(RedisConnectionFactory.class);
        for (RedisConnectionFactory factory : loader) {
            logger.info("Loaded Redis connection factory: {}", factory.getName());
            return factory;
        }
        
        // 如果SPI加载失败，使用默认的动态连接工厂
        logger.warn("No RedisConnectionFactory found via SPI, using DynamicRedisConnectionFactory as fallback");
        return new DynamicRedisConnectionFactory();
    }
}