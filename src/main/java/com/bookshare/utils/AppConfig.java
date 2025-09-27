package com.bookshare.utils;

import com.bookshare.document.cacher.BookInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ReactiveRedisTemplate<String, BookInfo> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<BookInfo> serializer = new Jackson2JsonRedisSerializer<>(BookInfo.class);
        RedisSerializationContext<String, BookInfo> context = RedisSerializationContext
                .<String, BookInfo>newSerializationContext(RedisSerializer.string())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
