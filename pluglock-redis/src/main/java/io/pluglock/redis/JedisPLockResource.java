package io.pluglock.redis;

import io.pluglock.redis.command.RedisCommandExecutor;
import io.pluglock.redis.command.jedis.JedisCommandExecutor;
import io.pluglock.redis.listener.LockReleaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

/**
 * 基于Jedis的Redis锁资源实现
 */
public class JedisPLockResource extends RedisPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(JedisPLockResource.class);
    
    // 订阅连接和监听器
    private volatile Jedis subscribeJedis;
    private volatile LockReleaseListener listener;
    private String host;
    private int port;
    
    public JedisPLockResource() {
        super();
        initHostPort();
    }
    
    public JedisPLockResource(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
        initHostPort();
    }
    
    private void initHostPort() {
        if (getCommandExecutor().getConnectionFactory() instanceof JedisConnectionFactory) {
            JedisConnectionFactory factory = (JedisConnectionFactory) getCommandExecutor().getConnectionFactory();
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
        return new JedisCommandExecutor(connectionFactory);
    }
    
    @Override
    protected void doSubscribe(String name) {
        String channelName = getChannelName(name);
        
        // 初始化订阅连接和监听器
        if (subscribeJedis == null) {
            synchronized (this) {
                if (subscribeJedis == null) {
                    subscribeJedis = new Jedis(host, port);
                    listener = new LockReleaseListener(this);
                }
            }
        }
        
        try {
            // 在单独的线程中执行订阅，因为subscribe方法是阻塞的
            CompletableFuture.runAsync(() -> {
                try {
                    subscribeJedis.subscribe(listener, channelName);
                } catch (Exception e) {
                    logger.error("Failed to subscribe to channel: {}", channelName, e);
                }
            });
            
            logger.debug("Subscribed to lock release notifications for: {} on channel: {}", name, channelName);
        } catch (Exception e) {
            logger.error("Error subscribing to lock release notifications for: {}", name, e);
        }
    }
    
    @Override
    protected void doUnsubscribe(String name) {
        String channelName = getChannelName(name);
        
        if (subscribeJedis != null && listener != null) {
            try {
                listener.unsubscribe(channelName);
                logger.debug("Unsubscribed from lock release notifications for: {} on channel: {}", name, channelName);
            } catch (Exception e) {
                logger.error("Error unsubscribing from lock release notifications for: {}", name, e);
            }
        }
    }

    @Override
    public Long tryAcquireResource(String name, long threadId, long leaseTime) {
        return 0L;
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