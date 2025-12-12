package io.pluglock.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于Lettuce的Redis锁资源实现
 */
public class LettucePLockResource extends RedisPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(LettucePLockResource.class);
    
    public LettucePLockResource() {
        super();
    }
    
    public LettucePLockResource(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
    
    @Override
    protected <T> T executeScript(RedisConnection<?> connection, String script, String[] keys, String... args) {
        if (!(connection instanceof LettuceConnection)) {
            throw new IllegalArgumentException("Connection must be an instance of LettuceConnection");
        }
        
        LettuceConnection lettuceConnection = (LettuceConnection) connection;
        io.lettuce.core.api.StatefulRedisConnection<String, String> nativeConnection = lettuceConnection.getNativeConnection();
        io.lettuce.core.api.sync.RedisCommands<String, String> commands = nativeConnection.sync();
        
        try {
            // Lettuce的eval方法接受ScriptOutputType、keys和args参数
            Object result = commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER, keys, args);
            return (T) result;
        } catch (Exception e) {
            logger.error("Failed to execute script with Lettuce", e);
            throw new RuntimeException("Failed to execute script with Lettuce", e);
        }
    }
}