package com.kakura.icetube.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class CacheConfiguration {

    public final static String BLACKLIST_CACHE_NAME = "jwt-black-list";

    public final static String REFRESH_CACHE_NAME = "jwt-refresh-list";

    public final static String DEFAULT_CACHE_NAME = "default-cache";

    @Value("${refresh_token_expiration_millis}")
    private int refreshTokenDuration;

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

@Bean
public RedisCacheManager redisCacheManager() {
    Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
    configurationMap.put(BLACKLIST_CACHE_NAME, RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(refreshTokenDuration)));
    configurationMap.put(REFRESH_CACHE_NAME, RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(refreshTokenDuration)));
    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory())
            .withInitialCacheConfigurations(configurationMap)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig());
    return builder.build();
}

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    @Primary
    public CacheManager compositeCacheManager() {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(cacheManager(), redisCacheManager());
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;
    }

}