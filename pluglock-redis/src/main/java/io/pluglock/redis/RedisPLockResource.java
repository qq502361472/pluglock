package io.pluglock.redis;

import io.pluglock.core.AbstractPLockResource;
import io.pluglock.core.PLockEntry;
import io.pluglock.redis.command.RedisCommandExecutor;
import io.pluglock.redis.spi.ConnectionFactoryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Redis锁资源抽象基类
 */
public abstract class RedisPLockResource extends AbstractPLockResource {
    private static final Logger logger = LoggerFactory.getLogger(RedisPLockResource.class);
    
    protected final RedisCommandExecutor commandExecutor;
    
    // 存储锁条目映射
    protected final Map<String, PLockEntry> lockEntries = new ConcurrentHashMap<>();
    
    // 定义获取锁的Lua脚本
    protected static final String ACQUIRE_SCRIPT = 
        "if (redis.call('exists', KEYS[1]) == 0) then " +
        "redis.call('hset', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
        "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "return redis.call('pttl', KEYS[1]);";
        
    // 定义尝试获取锁的Lua脚本
    protected static final String TRY_ACQUIRE_SCRIPT =
        "if (redis.call('exists', KEYS[1]) == 0) then " +
        "redis.call('hset', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
        "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return nil; " +
        "end; " +
        "return redis.call('pttl', KEYS[1]);";
        
    // 定义释放锁的Lua脚本
    protected static final String RELEASE_SCRIPT =
        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
        "return nil;" +
        "end; " +
        "local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1); " +
        "if (counter > 0) then " +
        "redis.call('pexpire', KEYS[1], ARGV[1]); " +
        "return 0; " +
        "else " +
        "redis.call('del', KEYS[1]); " +
        "redis.call('publish', KEYS[2], ARGV[3]); " +
        "return 1; " +
        "end; " +
        "return nil;";

    public RedisPLockResource() {
        this.commandExecutor = createCommandExecutor(ConnectionFactoryLoader.loadConnectionFactory());
    }
    
    public RedisPLockResource(RedisConnectionFactory connectionFactory) {
        this.commandExecutor = createCommandExecutor(connectionFactory);
    }
    
    /**
     * 创建命令执行器
     * 
     * @param connectionFactory 连接工厂
     * @return 命令执行器
     */
    protected abstract RedisCommandExecutor createCommandExecutor(RedisConnectionFactory connectionFactory);
    
    @Override
    public Long acquireResource(String name, long leaseTime, TimeUnit unit, long threadId) {
        try {
            return doAcquireResource(name, leaseTime, unit, threadId);
        } catch (Exception e) {
            logger.error("Failed to acquire Redis lock: {}", name, e);
            throw new RuntimeException("Failed to acquire Redis lock", e);
        }
    }
    
    private Long doAcquireResource(String name, long leaseTime, TimeUnit unit, long threadId) {
        return (Long) commandExecutor.executeEval(ACQUIRE_SCRIPT, new String[]{name}, 
                           String.valueOf(unit.toMillis(leaseTime)), String.valueOf(threadId));
    }
    
    @Override
    public PLockEntry subscribe(String name) {
        PLockEntry entry = new PLockEntry();
        lockEntries.put(name, entry);
        
        // 启动异步订阅任务
        CompletableFuture.runAsync(() -> {
            try {
                doSubscribe(name);
            } catch (Exception e) {
                logger.error("Failed to subscribe to lock release notifications for: {}", name, e);
            }
        });
        
        return entry;
    }
    
    /**
     * 执行订阅操作
     * 
     * @param name 锁名称
     */
    protected abstract void doSubscribe(String name);
    
    @Override
    public void unsubscribe(String name) {
        lockEntries.remove(name);
        doUnsubscribe(name);
    }
    
    /**
     * 执行取消订阅操作
     * 
     * @param name 锁名称
     */
    protected abstract void doUnsubscribe(String name);
    
    public Long tryAcquireResource(String name, long threadId) {
        try {
            return doTryAcquireResource(name, threadId);
        } catch (Exception e) {
            logger.error("Failed to try acquire Redis lock: {}", name, e);
            throw new RuntimeException("Failed to try acquire Redis lock", e);
        }
    }
    
    private Long doTryAcquireResource(String name, long threadId) {
        return (Long) commandExecutor.executeEval(TRY_ACQUIRE_SCRIPT, new String[]{name}, 
                           String.valueOf(30000), String.valueOf(threadId));
    }
    
    @Override
    public void releaseResource(String name, long threadId) {
        try {
            doReleaseResource(name, threadId);
        } catch (Exception e) {
            logger.error("Failed to release Redis lock: {}", name, e);
            throw new RuntimeException("Failed to release Redis lock", e);
        }
    }
    
    private void doReleaseResource(String name, long threadId) {
        String channelName = getChannelName(name);
        commandExecutor.executeEval(RELEASE_SCRIPT, new String[]{name, channelName}, 
                     String.valueOf(30000), String.valueOf(threadId), "1");
    }
    
    private String getChannelName(String lockName) {
        return "lock:" + lockName + ":channel";
    }
    
    @Override
    protected void startWatchDog(String name, long threadId, long millis, Long ttl) {
        // TODO: 实现看门狗逻辑
        logger.debug("Starting watchdog for lock: {}, threadId: {}", name, threadId);
    }
    
    /**
     * 获取命令执行器
     * @return Redis命令执行器
     */
    public RedisCommandExecutor getCommandExecutor() {
        return commandExecutor;
    }
    
    /**
     * 获取锁条目映射
     * @return 锁条目映射
     */
    public Map<String, PLockEntry> getLockEntries() {
        return lockEntries;
    }
}