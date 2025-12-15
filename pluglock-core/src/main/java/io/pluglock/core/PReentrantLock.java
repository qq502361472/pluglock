package io.pluglock.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 可重入分布式锁抽象基类
 */
public abstract class PReentrantLock extends AbstractPLock {

    /**
     * 锁资源
     */
    protected PLockResource lockResource;

    /**
     * 默认超时时间（30秒）
     */
    protected static final long DEFAULT_TIMEOUT_SECONDS = Constants.DEFAULT_TIMEOUT_SECONDS;

    public PReentrantLock(String lockName) {
        super(lockName);
    }

    @Override
    public void lock() {
        try {
            lock(-1, null);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        Long ttl = lockResource.acquireResource(getName(), leaseTime, unit, threadId);
        if (ttl == null) {
            // 上锁成功
            return;
        }
        // 这里阻塞订阅锁释放消息
        PLockEntry pLockEntry = lockResource.subscribe(getName());

        try {
            while (true) {
                ttl = lockResource.acquireResource(getName(), leaseTime, unit, threadId);
                if (ttl == null) {
                    // 上锁成功
                    break;
                }

                if (ttl >= 0) {
                    try {
                        // 阻塞ttl这么久
                        pLockEntry.getLatch().tryAcquire(ttl, TimeUnit.MICROSECONDS);
                    } catch (InterruptedException e) {

                    }
                } else {
                    // 即将到期直接一直阻塞
                    pLockEntry.getLatch().acquire();
                }
            }
        } finally {
            // 释放订阅
            lockResource.unsubscribe(getName());
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("Reentrant distributed lock does not support lockInterruptibly");
    }

    @Override
    public boolean tryLock() {
        // 修正此处调用，添加缺少的参数
        Long ttl = lockResource.tryAcquireResource(getName(), Thread.currentThread().getId(), DEFAULT_TIMEOUT_SECONDS * 1000);
        return ttl != null;
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Reentrant distributed lock does not support tryLock");
    }

    @Override
    public void unlock() {
        lockResource.releaseResource(getName(), Thread.currentThread().getId());
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Reentrant distributed lock does not support Condition");
    }

}