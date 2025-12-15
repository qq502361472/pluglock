package io.pluglock.redis.command;

import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.RedisConnectionFactory;

/**
 * Redis命令执行器接口
 */
public interface RedisCommandExecutor {
    
    /**
     * 执行EVAL命令
     * 
     * @param script Lua脚本
     * @param keys 键数组
     * @param args 参数数组
     * @return 执行结果
     */
    Object executeEval(String script, String[] keys, String... args);
    
    /**
     * 获取连接工厂
     * 
     * @return Redis连接工厂
     */
    RedisConnectionFactory getConnectionFactory();
}