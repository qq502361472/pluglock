package io.pluglock.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 锁抽象基类，提供基础实现
 */
public abstract class AbstractPLock implements PLock {
    
    protected final String lockName;
    
    public AbstractPLock(String lockName) {
        this.lockName = lockName;
    }
    
    @Override
    public void lock() {
        while (!tryLock()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while acquiring lock", e);
            }
        }
    }
    
    @Override
    public void lockInterruptibly() throws InterruptedException {
        while (!tryLock()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted while acquiring lock");
            }
            Thread.sleep(100);
        }
    }
    
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long timeoutMillis = unit.toMillis(time);
        long startTime = System.currentTimeMillis();
        
        while (true) {
            if (tryLock()) {
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
    public Condition newCondition() {
        throw new UnsupportedOperationException("Distributed lock does not support Condition");
    }
    
    @Override
    public String getName() {
        return lockName;
    }
}