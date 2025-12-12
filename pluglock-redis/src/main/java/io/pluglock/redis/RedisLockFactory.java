package io.pluglock.redis;

import io.pluglock.core.LockConfig;
import io.pluglock.core.LockFactory;
import io.pluglock.core.PLock;
import io.pluglock.core.PReentrantLock;
import io.pluglock.core.PLockResource;

/**
 * Redis分布式锁工厂实现
 */
public class RedisLockFactory implements LockFactory {
    @Override
    public PLock createLock(String name, LockConfig config) {
        // 默认使用Jedis实现，也可以根据配置选择Lettuce实现
        PLockResource lockResource = new JedisPLockResource();
        return new RedisLock(name, lockResource);
    }

    @Override
    public String getName() {
        return "redis";
    }
    
    /**
     * 基于Redis的具体锁实现
     */
    private static class RedisLock extends PReentrantLock {
        public RedisLock(String lockName, PLockResource lockResource) {
            super(lockName);
            this.lockResource = lockResource;
        }
    }
}