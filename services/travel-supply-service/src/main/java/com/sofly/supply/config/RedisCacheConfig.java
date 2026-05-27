package com.sofly.supply.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@EnableCaching
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 항공/호텔 검색 결과: JsonNode, TTL 5분
        RedisCacheConfiguration searchConfig = jsonNodeCacheConfig(Duration.ofMinutes(5));

        // 목적지/정렬/필터 메타 데이터: POJO, TTL 24시간
        RedisCacheConfiguration pojoMetaConfig = pojoCacheConfig(Duration.ofHours(24));

        // getFilter는 JsonNode 반환, TTL 24시간
        RedisCacheConfiguration jsonNodeMetaConfig = jsonNodeCacheConfig(Duration.ofHours(24));

        // 장소 검색 결과: POJO, TTL 1시간
        RedisCacheConfiguration placeSearchConfig = pojoCacheConfig(Duration.ofHours(1));

        // 장소 사진 URL: POJO, TTL 24시간 (URL이 잘 안 바뀜)
        RedisCacheConfiguration placePhotoConfig = pojoCacheConfig(Duration.ofHours(24));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(searchConfig)
                .withInitialCacheConfigurations(Map.of(
                        "flightDestinations", pojoMetaConfig,
                        "hotelDestinations",  pojoMetaConfig,
                        "hotelSortBy",        pojoMetaConfig,
                        "hotelFilter",        jsonNodeMetaConfig,
                        "placeSearch",        placeSearchConfig,
                        "placePhoto",         placePhotoConfig
                ))
                .build();
    }

    private RedisCacheConfiguration jsonNodeCacheConfig(Duration ttl) {
        Jackson2JsonRedisSerializer<JsonNode> serializer =
                new Jackson2JsonRedisSerializer<>(new ObjectMapper(), JsonNode.class);
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }

    private RedisCacheConfiguration pojoCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
