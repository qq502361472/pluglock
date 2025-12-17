package io.pluglock.core;

import java.util.concurrent.TimeUnit;

public abstract class AbstractPLockResource implements PLockResource {
    /**
     * 锁资源持有的自动过期时间
     */
    private volatile long intervalLeaseTime;

    @Override
    public Long acquireResource(String name, long leaseTime, TimeUnit unit, long threadId) {
        long sourceLeaseTime = leaseTime;
        if (leaseTime == -1) {
            sourceLeaseTime = 30 * 1000;
        }
        long leaseMillis = unit.toMillis(sourceLeaseTime);
        Long ttl = tryAcquireResource(name, threadId, leaseMillis);
        if (ttl == null) {
            if (leaseTime == -1) {
                // 启动监控狗
                startWatchDog(name, threadId, leaseMillis, ttl);
            } else {
                // 什么也不用做,紧做记录
                intervalLeaseTime = leaseMillis;
            }
            return ttl;
        }
        return 0L;
    }

    protected abstract void startWatchDog(String name, long threadId, long leaseMillis, Long ttl);

    public long getIntervalLeaseTime() {
        return intervalLeaseTime;
    }
}
