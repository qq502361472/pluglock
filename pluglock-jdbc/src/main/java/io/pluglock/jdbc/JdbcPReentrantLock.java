package io.pluglock.jdbc;

import io.pluglock.core.PLockEntry;
import io.pluglock.core.PReentrantLock;

import java.util.concurrent.TimeUnit;

/**
 * 基于JDBC的可重入锁实现
 */
public class JdbcPReentrantLock extends PReentrantLock {
    
    private final JdbcHelper jdbcHelper;
    private final int expireTimeSeconds;
    
    public JdbcPReentrantLock(String lockName, JdbcHelper jdbcHelper) {
        this(lockName, jdbcHelper, 30);
    }
    
    public JdbcPReentrantLock(String lockName, JdbcHelper jdbcHelper, int expireTimeSeconds) {
        super(lockName);
        this.jdbcHelper = jdbcHelper;
        this.expireTimeSeconds = expireTimeSeconds;
        // 初始化 lockResource 字段
        this.lockResource = new JdbcPLockResource(jdbcHelper);
    }

    /**
     * JDBC锁资源实现
     */
    private static class JdbcPLockResource extends io.pluglock.core.AbstractPLockResource {
        private final JdbcHelper jdbcHelper;

        public JdbcPLockResource(JdbcHelper jdbcHelper) {
            this.jdbcHelper = jdbcHelper;
        }

        @Override
        public Long tryAcquireResource(String name, long threadId, long leaseTime) {
            // 修正方法调用，使用正确的API
            boolean acquired = jdbcHelper.tryAcquireLock(name, String.valueOf(threadId), (int) TimeUnit.MILLISECONDS.toSeconds(leaseTime));
            return acquired ? null : 0L;
        }

        @Override
        public PLockEntry subscribe(String name) {
            // JDBC实现不支持订阅机制，直接返回一个空的PLockEntry
            return new PLockEntry();
        }

        @Override
        public void unsubscribe(String name) {
            // JDBC实现不支持订阅机制，无需执行任何操作
        }

        @Override
        public void releaseResource(String name, long threadId) {
            jdbcHelper.releaseLock(name, String.valueOf(threadId));
        }

        @Override
        protected void startWatchDog(String name, long threadId, long leaseMillis, Long ttl) {
            // JDBC实现暂不支持看门狗机制
        }
    }
}