package io.pluglock.jdbc;

import io.pluglock.core.AbstractPLock;

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
    public boolean tryLock() {
        return jdbcHelper.tryAcquireLock(lockName, lockValue, expireTimeSeconds);
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