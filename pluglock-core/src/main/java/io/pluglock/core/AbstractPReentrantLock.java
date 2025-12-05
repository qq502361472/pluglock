package io.pluglock.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 可重入锁抽象基类
 */
public abstract class AbstractPReentrantLock extends AbstractPLock {
    
    private final ThreadLocal<Integer> lockCount = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    
    protected static final ConcurrentHashMap<String, String> LOCK_HOLDER = new ConcurrentHashMap<>();
    
    public AbstractPReentrantLock(String lockName) {
        super(lockName);
    }
    
    @Override
    public void lock() {
        int count = lockCount.get();
        if (count > 0) {
            // 如果当前线程已经持有锁，增加重入计数
            lockCount.set(count + 1);
            return;
        }
        
        String lockValue = "locked-by-" + Thread.currentThread().getName();
        while (!acquireLock(lockValue)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while acquiring lock", e);
            }
        }
        
        // 成功获取锁，记录锁持有者
        LOCK_HOLDER.put(lockName, lockValue);
        lockCount.set(1);
    }
    
    @Override
    public void lockInterruptibly() throws InterruptedException {
        int count = lockCount.get();
        if (count > 0) {
            // 如果当前线程已经持有锁，增加重入计数
            lockCount.set(count + 1);
            return;
        }
        
        String lockValue = "locked-by-" + Thread.currentThread().getName();
        while (!acquireLock(lockValue)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted while acquiring lock");
            }
            Thread.sleep(100);
        }
        
        // 成功获取锁，记录锁持有者
        LOCK_HOLDER.put(lockName, lockValue);
        lockCount.set(1);
    }
    
    @Override
    public boolean tryLock() {
        int count = lockCount.get();
        if (count > 0) {
            // 如果当前线程已经持有锁，增加重入计数
            lockCount.set(count + 1);
            return true;
        }
        
        String lockValue = "locked-by-" + Thread.currentThread().getName();
        boolean acquired = acquireLock(lockValue);
        if (acquired) {
            // 成功获取锁，记录锁持有者
            LOCK_HOLDER.put(lockName, lockValue);
            lockCount.set(1);
        }
        return acquired;
    }
    
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        int count = lockCount.get();
        if (count > 0) {
            // 如果当前线程已经持有锁，增加重入计数
            lockCount.set(count + 1);
            return true;
        }
        
        long timeoutMillis = unit.toMillis(time);
        long startTime = System.currentTimeMillis();
        String lockValue = "locked-by-" + Thread.currentThread().getName();
        
        while (true) {
            boolean acquired = acquireLock(lockValue);
            if (acquired) {
                // 成功获取锁，记录锁持有者
                LOCK_HOLDER.put(lockName, lockValue);
                lockCount.set(1);
                return true;
            }
            
            if (System.currentTimeMillis() - startTime >= timeoutMillis) {
                return false;
            }
            
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted while acquiring lock");
            }
            
            Thread.sleep(100);
        }
    }
    
    @Override
    public void unlock() {
        int count = lockCount.get();
        if (count <= 0) {
            throw new IllegalMonitorStateException("Current thread does not hold the lock");
        }
        
        if (count > 1) {
            // 只减少重入计数，不释放锁
            lockCount.set(count - 1);
            return;
        }
        
        // 重入计数为1，需要真正释放锁
        String lockValue = LOCK_HOLDER.get(lockName);
        if (lockValue != null && lockValue.equals("locked-by-" + Thread.currentThread().getName())) {
            releaseLock(lockValue);
            LOCK_HOLDER.remove(lockName);
        }
        
        lockCount.set(0);
    }
    
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Reentrant distributed lock does not support Condition");
    }
    
    /**
     * 获取锁的具体实现，由子类实现
     * 
     * @param lockValue 锁的值
     * @return 是否成功获取锁
     */
    protected abstract boolean acquireLock(String lockValue);
    
    /**
     * 释放锁的具体实现，由子类实现
     * 
     * @param lockValue 锁的值
     */
    protected abstract void releaseLock(String lockValue);
}