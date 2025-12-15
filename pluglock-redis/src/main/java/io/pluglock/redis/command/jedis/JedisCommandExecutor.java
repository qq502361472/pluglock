package io.pluglock.redis.command.jedis;

import io.pluglock.redis.JedisConnection;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.command.AbstractRedisCommandExecutor;
import redis.clients.jedis.Jedis;

/**
 * Jedis命令执行器
 */
public class JedisCommandExecutor extends AbstractRedisCommandExecutor {
    
    public JedisCommandExecutor(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
    
    @Override
    protected Object doExecuteEval(RedisConnection<?> connection, String script, String[] keys, String[] args) {
        if (!(connection instanceof JedisConnection)) {
            throw new IllegalArgumentException("Connection must be an instance of JedisConnection");
        }
        
        Jedis jedis = ((JedisConnection) connection).getNativeConnection();
        return jedis.eval(script, keys.length, mergeArrays(keys, args));
    }
    
    private static String[] mergeArrays(String[] keys, String[] args) {
        String[] result = new String[keys.length + args.length];
        System.arraycopy(keys, 0, result, 0, keys.length);
        System.arraycopy(args, 0, result, keys.length, args.length);
        return result;
    }
}