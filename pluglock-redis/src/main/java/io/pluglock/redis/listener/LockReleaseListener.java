package io.pluglock.redis.listener;

import io.pluglock.core.PLockEntry;
import io.pluglock.redis.RedisPLockResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * 锁释放监听器
 * 监听Redis发布的锁释放消息，并通知等待的线程
 */
public class LockReleaseListener extends JedisPubSub {
    
    private static final Logger logger = LoggerFactory.getLogger(LockReleaseListener.class);
    
    private final RedisPLockResource lockResource;
    
    public LockReleaseListener(RedisPLockResource lockResource) {
        this.lockResource = lockResource;
    }
    
    @Override
    public void onMessage(String channel, String message) {
        logger.debug("Received lock release message on channel: {}, message: {}", channel, message);
        
        // 从通道名解析出锁名称
        String lockName = parseLockNameFromChannel(channel);
        if (lockName != null) {
            Map<String, PLockEntry> lockEntries = lockResource.getLockEntries();
            PLockEntry entry = lockEntries.get(lockName);
            if (entry != null) {
                Semaphore latch = entry.getLatch();
                if (latch != null) {
                    // 释放信号量，唤醒等待的线程
                    latch.release();
                    logger.debug("Released semaphore for lock: {}", lockName);
                }
            }
        }
    }
    
    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        logger.debug("Subscribed to channel: {}, total channels: {}", channel, subscribedChannels);
    }
    
    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        logger.debug("Unsubscribed from channel: {}, total channels: {}", channel, subscribedChannels);
    }
    
    /**
     * 从通道名解析出锁名称
     * 
     * @param channel 通道名
     * @return 锁名称
     */
    private String parseLockNameFromChannel(String channel) {
        if (channel != null && channel.startsWith("lock:") && channel.endsWith(":channel")) {
            return channel.substring(5, channel.length() - 8);
        }
        return null;
    }
}