package io.pluglock.redis;

/**
 * Redis连接工厂接口
 */
public interface RedisConnectionFactory {
    
    /**
     * 获取Redis连接
     * 
     * @param <T> Redis客户端连接类型
     * @return Redis连接
     */
    <T> RedisConnection<T> getConnection();
    
    /**
     * 释放Redis连接
     * 
     * @param connection Redis连接
     */
    void releaseConnection(RedisConnection<?> connection);
    
    /**
     * 销毁连接工厂，关闭相关资源
     */
    void destroy();
    
    /**
     * 获取工厂名称
     * 
     * @return 工厂名称
     */
    String getName();
}