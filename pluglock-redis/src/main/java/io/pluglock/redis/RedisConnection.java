package io.pluglock.redis;

import io.pluglock.core.StorageCallback;

/**
 * Redis连接接口
 * 
 * @param <T> Redis客户端连接类型
 */
public interface RedisConnection<T> {
    
    /**
     * 执行Redis操作
     * 
     * @param callback Redis操作回调
     * @param <R> 返回值类型
     * @return 操作结果
     */
    <R> R execute(StorageCallback<T, R> callback);
    
    /**
     * 关闭连接
     */
    void close();
    
    /**
     * 获取原生Redis连接
     * 
     * @return 原生Redis连接
     */
    T getNativeConnection();
}