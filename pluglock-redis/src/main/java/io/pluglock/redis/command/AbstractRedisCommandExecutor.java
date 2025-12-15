package io.pluglock.redis.command;

import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis命令执行器抽象基类
 */
public abstract class AbstractRedisCommandExecutor implements RedisCommandExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractRedisCommandExecutor.class);
    
    protected final RedisConnectionFactory connectionFactory;
    
    public AbstractRedisCommandExecutor(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    @Override
    public Object executeEval(String script, String[] keys, String... args) {
        RedisConnection<?> connection = null;
        try {
            connection = connectionFactory.getConnection();
            return doExecuteEval(connection, script, keys, args);
        } catch (Exception e) {
            logger.error("Failed to execute Redis script", e);
            throw new RuntimeException("Failed to execute Redis script", e);
        } finally {
            if (connection != null) {
                try {
                    connectionFactory.releaseConnection(connection);
                } catch (Exception e) {
                    logger.warn("Failed to release Redis connection", e);
                }
            }
        }
    }
    
    /**
     * 执行EVAL命令的具体实现，由子类提供
     * 
     * @param connection Redis连接
     * @param script Lua脚本
     * @param keys 键数组
     * @param args 参数数组
     * @return 执行结果
     */
    protected abstract Object doExecuteEval(RedisConnection<?> connection, String script, String[] keys, String[] args);
    
    @Override
    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}