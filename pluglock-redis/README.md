# PlugLock Redis Module

Redis module for PlugLock distributed locking framework.

## Features

- Support for both Jedis and Lettuce Redis clients
- Dynamic client selection mechanism based on classpath detection
- Connection pooling for improved performance
- Reentrant distributed locks
- Automatic lock expiration handling

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>io.pluglock</groupId>
    <artifactId>pluglock-redis</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Selecting Redis Client via Classpath

PlugLock Redis supports both Jedis and Lettuce clients. To select which one to use, include only one of the following dependencies in your project:

#### For Jedis

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.0.0</version>
</dependency>
```

#### For Lettuce

```xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.1.5.RELEASE</version>
</dependency>
```

The framework automatically detects which client is available on the classpath and uses it accordingly. If both clients are present, Jedis takes precedence.

### Connection Pooling

Both Jedis and Lettuce implementations use connection pooling for optimal performance:

- Jedis: Built-in JedisPool
- Lettuce: Apache Commons Pool2 integration

Connection pool configuration:
- Max total connections: 20
- Max idle connections: 10
- Min idle connections: 2

### Configuration

TODO: Add configuration examples

## Implementation Details

The Redis module implements the core `PLockResource` interface and provides a dynamic implementation that automatically selects the appropriate Redis client based on what's available in the classpath.

The implementation hierarchy:
1. `RedisPLockResource` - Main lock resource implementation
2. `DynamicRedisConnectionFactory` - Automatically selects between Jedis and Lettuce
3. `JedisConnectionFactoryImpl` - Jedis-specific connection factory
4. `LettuceConnectionFactoryImpl` - Lettuce-specific connection factory

Dependencies:
- Both Jedis and Lettuce dependencies are marked as `provided` scope in pluglock-redis
- Apache Commons Pool2 is included with `compile` scope for connection pooling support