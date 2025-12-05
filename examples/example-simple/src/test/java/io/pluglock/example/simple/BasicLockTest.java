package io.pluglock.example.simple;

import io.pluglock.core.DefaultLockManager;
import io.pluglock.core.PLock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础分布式锁测试类
 * 展示创建锁、上锁、释放锁的基本范式
 */
class BasicLockTest {

    @Test
    void testCreateAndUseLock() {
        // 创建分布式锁实例
        PLock lock = DefaultLockManager.createLock("test-basic-lock");
        
        // 验证锁实例不为空
        assertNotNull(lock, "锁实例不应为null");
        
        // 上锁
        lock.lock();
        
        try {
            // 执行业务逻辑（这里只是简单示例）
            assertTrue(true, "业务逻辑执行成功");
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
    
    @Test
    void testTryLock() {
        // 创建分布式锁实例
        PLock lock = DefaultLockManager.createLock("test-try-lock");
        
        // 尝试获取锁
        boolean locked = lock.tryLock();
        assertTrue(locked, "应该能够成功获取锁");
        
        try {
            // 再次尝试获取锁（同一个线程）
            boolean lockedAgain = lock.tryLock();
            // 结果取决于锁的具体实现（可重入锁 vs 非可重入锁）
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
}