package io.pluglock.redis;

import io.pluglock.core.StorageCallback;
import io.pluglock.core.StorageOperation;

/**
 * Redis操作助手接口，定义了Redis操作的基本方法
 */
public interface RedisHelper extends StorageOperation<Object> {
    
    /**
     * 尝试获取锁
     * 
     * @param key 锁的键名
     * @param value 锁的值
     * @param expireSeconds 过期时间（秒）
     * @return 是否成功获取锁
     */
    boolean tryAcquireLock(String key, String value, int expireSeconds);
    
    /**
     * 释放锁
     * 
     * @param key 锁的键名
     * @param value 锁的值
     * @return 是否成功释放锁
     */
    boolean releaseLock(String key, String value);
    
    /**
     * 检查锁是否被占用
     * 
     * @param key 锁的键名
     * @return 锁是否被占用
     */
    boolean isLocked(String key);
}