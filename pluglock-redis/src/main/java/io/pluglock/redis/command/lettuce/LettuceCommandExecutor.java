package io.pluglock.redis.command.lettuce;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.pluglock.redis.LettuceConnection;
import io.pluglock.redis.RedisConnection;
import io.pluglock.redis.RedisConnectionFactory;
import io.pluglock.redis.command.AbstractRedisCommandExecutor;

/**
 * Lettuce命令执行器
 */
public class LettuceCommandExecutor extends AbstractRedisCommandExecutor {
    
    public LettuceCommandExecutor(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
    
    @Override
    protected Object doExecuteEval(RedisConnection<?> connection, String script, String[] keys, String[] args) {
        if (!(connection instanceof LettuceConnection)) {
            throw new IllegalArgumentException("Connection must be an instance of LettuceConnection");
        }
        
        StatefulRedisConnection<String, String> lettuceConnection = ((LettuceConnection) connection).getNativeConnection();
        RedisCommands<String, String> commands = lettuceConnection.sync();
        return commands.eval(script, ScriptOutputType.INTEGER, keys, args);
    }
}