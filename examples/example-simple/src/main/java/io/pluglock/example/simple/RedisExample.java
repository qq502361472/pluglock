package io.pluglock.example.simple;

import io.pluglock.core.DefaultLockManager;
import io.pluglock.core.LockConfig;
import io.pluglock.core.LockManager;
import io.pluglock.core.PLock;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis锁示例演示如何使用PlugLock框架的Redis实现
 */
public class RedisExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("PlugLock Redis示例开始");

        // 创建Redis锁配置
        LockConfig config = createRedisConfig();

        // 演示Redis锁的使用
        demonstrateRedisLock(config);

        System.out.println("PlugLock Redis示例结束");
    }

    private static LockConfig createRedisConfig() {
        Properties properties = new Properties();
        properties.setProperty("redis.host", "localhost");
        properties.setProperty("redis.port", "6379");
        properties.setProperty("redis.client", "jedis");
        return new LockConfig(properties);
    }

    private static void demonstrateRedisLock(LockConfig config) throws InterruptedException {
        System.out.println("\n=== Redis锁使用演示 ===");

        try {
            // 创建Redis锁
            PLock lock = DefaultLockManager.createLock("redis-demo-lock", config);
            System.out.println("创建Redis锁成功: " + lock.getName());

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(3);
            CountDownLatch latch = new CountDownLatch(3);

            // 启动3个并发线程
            for (int i = 0; i < 3; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        System.out.println("任务 " + taskId + " 开始执行");
                        
                        // 尝试获取锁
                        System.out.println("任务 " + taskId + " 尝试获取锁");
                        lock.lock(); // 获取锁
                        System.out.println("任务 " + taskId + " 获取到锁");
                        
                        // 模拟业务处理时间
                        Thread.sleep(2000);
                        System.out.println("任务 " + taskId + " 处理完成");
                        
                        lock.unlock(); // 释放锁
                        System.out.println("任务 " + taskId + " 释放锁");
                    } catch (Exception e) {
                        System.err.println("任务 " + taskId + " 发生异常: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Throwable t) {
                        System.err.println("任务 " + taskId + " 发生严重错误: " + t.getMessage());
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有任务完成
            latch.await();
            executor.shutdown();
            System.out.println("所有Redis并发任务执行完毕");
        } catch (Exception e) {
            System.err.println("Redis锁操作异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}