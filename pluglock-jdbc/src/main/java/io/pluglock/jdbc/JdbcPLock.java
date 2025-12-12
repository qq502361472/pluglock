package io.pluglock.jdbc;

import io.pluglock.core.AbstractPLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 基于JDBC的锁实现
 */
public class JdbcPLock extends AbstractPLock {
    
    private final JdbcHelper jdbcHelper;
    private final String lockValue;
    private final int expireTimeSeconds;
    
    public JdbcPLock(String lockName, JdbcHelper jdbcHelper) {
        this(lockName, jdbcHelper, 30);
    }
    
    public JdbcPLock(String lockName, JdbcHelper jdbcHelper, int expireTimeSeconds) {
        super(lockName);
        this.jdbcHelper = jdbcHelper;
        this.lockValue = "locked-by-" + Thread.currentThread().getName();
        this.expireTimeSeconds = expireTimeSeconds;
    }
    
    @Override
    public void lock() {
        // 简单实现，不断尝试获取锁直到成功
        while (!tryLock()) {
            try {
                Thread.sleep(100); // 等待100毫秒后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public void lock(long leaseTime, TimeUnit unit) throws InterruptedException {
        // 对于JDBC锁，我们忽略租约时间，直接使用lock()
        lock();
    }
    
    @Override
    public void lockInterruptibly() throws InterruptedException {
        // 简单实现，不断尝试获取锁直到成功或被中断
        while (!tryLock()) {
            Thread.sleep(100); // 等待100毫秒后重试
        }
    }
    
    @Override
    public boolean tryLock() {
        return jdbcHelper.tryAcquireLock(lockName, lockValue, expireTimeSeconds);
    }
    
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(time);
        while (System.currentTimeMillis() < endTime) {
            if (tryLock()) {
                return true;
            }
            Thread.sleep(100); // 等待100毫秒后重试
        }
        return false;
    }
    
    @Override
    public void unlock() {
        jdbcHelper.releaseLock(lockName, lockValue);
    }
    
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("JDBC lock does not support Condition");
    }
}