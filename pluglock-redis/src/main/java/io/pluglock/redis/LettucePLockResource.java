package io.pluglock.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.pluglock.core.PLockEntry;
import io.pluglock.redis.command.RedisCommandExecutor;
import io.pluglock.redis.command.lettuce.LettuceCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * 基于Lettuce的Redis锁资源实现
 */
public class LettucePLockResource extends RedisPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(LettucePLockResource.class);
    
    // 订阅连接和命令
    private volatile StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private volatile RedisPubSubCommands<String, String> pubSubCommands;
    private String host;
    private int port;
    
    public LettucePLockResource() {
        super();
        initHostPort();
    }
    
    public LettucePLockResource(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
        initHostPort();
    }
    
    private void initHostPort() {
        if (getCommandExecutor().getConnectionFactory() instanceof LettuceConnectionFactory) {
            LettuceConnectionFactory factory = (LettuceConnectionFactory) getCommandExecutor().getConnectionFactory();
            this.host = factory.getHost();
            this.port = factory.getPort();
        } else {
            // 默认值
            this.host = "localhost";
            this.port = 6379;
        }
    }
    
    @Override
    protected RedisCommandExecutor createCommandExecutor(RedisConnectionFactory connectionFactory) {
        return new LettuceCommandExecutor(connectionFactory);
    }
    
    @Override
    protected void doSubscribe(String name) {
        String channelName = getChannelName(name);
        
        // 初始化订阅连接
        if (pubSubConnection == null) {
            synchronized (this) {
                if (pubSubConnection == null) {
                    // 创建一个新的Redis客户端用于订阅
                    RedisClient redisClient = RedisClient.create(
                        "redis://" + host + ":" + port
                    );
                    pubSubConnection = redisClient.connectPubSub();
                    pubSubCommands = pubSubConnection.sync();
                    
                    // 注册消息监听器
                    pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
                        @Override
                        public void message(String channel, String message) {
                            logger.debug("Received lock release message on channel: {}, message: {}", channel, message);
                            
                            // 从通道名解析出锁名称
                            String lockName = parseLockNameFromChannel(channel);
                            if (lockName != null) {
                                PLockEntry entry = getLockEntries().get(lockName);
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
                        public void subscribed(String channel, long count) {
                            logger.debug("Subscribed to channel: {}, total channels: {}", channel, count);
                        }
                        
                        @Override
                        public void unsubscribed(String channel, long count) {
                            logger.debug("Unsubscribed from channel: {}, total channels: {}", channel, count);
                        }
                    });
                }
            }
        }
        
        try {
            pubSubCommands.subscribe(channelName);
            logger.debug("Subscribed to lock release notifications for: {} on channel: {}", name, channelName);
        } catch (Exception e) {
            logger.error("Error subscribing to lock release notifications for: {}", name, e);
        }
    }
    
    @Override
    protected void doUnsubscribe(String name) {
        String channelName = getChannelName(name);
        
        if (pubSubCommands != null) {
            try {
                pubSubCommands.unsubscribe(channelName);
                logger.debug("Unsubscribed from lock release notifications for: {} on channel: {}", name, channelName);
            } catch (Exception e) {
                logger.error("Error unsubscribing from lock release notifications for: {}", name, e);
            }
        }
    }

    @Override
    public Long tryAcquireResource(String name, long threadId, long leaseTime) {
        return null;
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
    
    /**
     * 获取通道名称
     * 
     * @param lockName 锁名称
     * @return 通道名称
     */
    private String getChannelName(String lockName) {
        return "lock:" + lockName + ":channel";
    }
}