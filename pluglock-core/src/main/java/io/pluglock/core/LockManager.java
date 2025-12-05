package io.pluglock.core;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 锁管理器，通过SPI机制加载和管理不同的锁工厂
 */
public class LockManager {
    
    private static final ConcurrentMap<String, LockFactory> lockFactories = new ConcurrentHashMap<>();
    // 定义默认锁类型的优先级顺序
    private static final String[] DEFAULT_LOCK_TYPE_PRIORITY = {"redis", "zookeeper", "jdbc"};
    
    static {
        loadLockFactories();
    }
    
    private static void loadLockFactories() {
        ServiceLoader<LockFactory> loader = ServiceLoader.load(LockFactory.class);
        for (LockFactory factory : loader) {
            lockFactories.put(factory.getName(), factory);
        }
    }
    
    /**
     * 根据类型获取锁工厂
     * 
     * @param type 锁类型（如"redis"、"jdbc"、"zookeeper"等）
     * @return 锁工厂实例
     */
    public static LockFactory getLockFactory(String type) {
        return lockFactories.get(type);
    }
    
    /**
     * 自动获取锁工厂，根据预设的优先级顺序选择第一个可用的锁工厂
     * 
     * @return 锁工厂实例，如果没有可用的锁工厂则返回null
     */
    public static LockFactory getLockFactory() {
        for (String type : DEFAULT_LOCK_TYPE_PRIORITY) {
            LockFactory factory = lockFactories.get(type);
            if (factory != null) {
                return factory;
            }
        }
        // 如果没有找到预设类型中的任何一个，则返回找到的第一个工厂（如果有的话）
        return lockFactories.values().stream().findFirst().orElse(null);
    }
    
    /**
     * 创建分布式锁
     * 
     * @param type 锁类型
     * @param name 锁名称
     * @param config 锁配置
     * @return 分布式锁实例
     */
    public static PLock createLock(String type, String name, LockConfig config) {
        LockFactory factory = getLockFactory(type);
        if (factory == null) {
            throw new IllegalArgumentException("No lock factory found for type: " + type);
        }
        return factory.createLock(name, config);
    }
    
    /**
     * 创建分布式锁，自动选择可用的锁工厂
     * 
     * @param name 锁名称
     * @param config 锁配置
     * @return 分布式锁实例
     */
    public static PLock createLock(String name, LockConfig config) {
        LockFactory factory = getLockFactory();
        if (factory == null) {
            throw new IllegalStateException("No lock factory available");
        }
        return factory.createLock(name, config);
    }
    
    /**
     * 获取所有可用的锁类型
     * 
     * @return 锁类型数组
     */
    public static String[] getAvailableLockTypes() {
        return lockFactories.keySet().toArray(new String[0]);
    }
}