package io.pluglock.jdbc;

import io.pluglock.core.PLock;
import io.pluglock.core.LockConfig;
import io.pluglock.core.LockFactory;

/**
 * JDBC分布式锁工厂实现
 */
public class JdbcLockFactory implements LockFactory {
    
    @Override
    public PLock createLock(String name, LockConfig config) {
        // 创建JDBC连接工厂
        String url = config.getProperty("jdbc.url");
        String username = config.getProperty("jdbc.username");
        String password = config.getProperty("jdbc.password");
        
        JdbcConnectionFactory connectionFactory = new JdbcConnectionFactory(url, username, password);
        JdbcHelper jdbcHelper = new JdbcHelper(connectionFactory);
        
        // 根据配置决定锁的类型
        String lockType = config.getProperty("jdbc.lock.type", "basic");
        if ("reentrant".equalsIgnoreCase(lockType)) {
            return new JdbcPReentrantLock(name, jdbcHelper);
        } else {
            return new JdbcPLock(name, jdbcHelper);
        }
    }
    
    @Override
    public String getName() {
        return "jdbc";
    }
}