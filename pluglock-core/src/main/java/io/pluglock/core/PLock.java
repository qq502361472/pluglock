package io.pluglock.core;

import java.util.concurrent.locks.Lock;

/**
 * 锁接口，继承自JDK的Lock接口
 */
public interface PLock extends Lock {

    /**
     * 获取锁的名字
     * 
     * @return 锁的名字
     */
    String getName();
}