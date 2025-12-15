package io.pluglock.core;

import java.util.concurrent.TimeUnit;

/**
 * 锁资源接口
 */
public interface PLockResource {
    /**
     * 获取锁资源
     *
     * @param name      锁资源名称
     * @param leaseTime 锁资源有效期
     * @param unit      锁资源有效期单位
     * @param threadId  线程ID
     * @return 锁过期时间
     */
    Long acquireResource(String name, long leaseTime, TimeUnit unit, long threadId);

    /**
     * 订阅锁释放的消息
     *
     * @param name 锁名称
     * @return
     */
    PLockEntry subscribe(String name);

    /**
     * 取消订阅锁释放的消息
     *
     * @param name 锁名称
     */
    void unsubscribe(String name);

    /**
     * 尝试获取锁资源
     *
     * @param name      锁资源名称
     * @param threadId  线程ID
     * @param leaseTime 锁资源有效期
     * @return 锁过期时间
     */
    Long tryAcquireResource(String name, long threadId, long leaseTime);

    /**
     * 释放锁资源
     *
     * @param name      锁资源名称
     * @param threadId  线程ID
     */
    void releaseResource(String name, long threadId);
}
