package io.pluglock.redis;

import io.pluglock.core.LockConfig;
import io.pluglock.core.LockFactory;
import io.pluglock.core.PLock;

/**
 * Redis分布式锁工厂实现
 */
public class RedisLockFactory implements LockFactory {
    @Override
    public PLock createLock(String name, LockConfig config) {
        // TODO: 实现Redis锁的创建逻辑
        throw new UnsupportedOperationException("Redis lock not implemented yet");
    }

    @Override
    public String getName() {
        return "redis";
    }
}