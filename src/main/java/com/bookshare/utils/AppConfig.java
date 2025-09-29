package com.bookshare.utils;

import com.bookshare.wrapper.response.BookResponse;
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

    @Bean(name = "bookRedisTemplate")
    public ReactiveRedisTemplate<String, BookResponse> reactiveRedisTemplateBook(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<BookResponse> serializer = new Jackson2JsonRedisSerializer<>(BookResponse.class);
        RedisSerializationContext<String, BookResponse> context = RedisSerializationContext
                .<String, BookResponse>newSerializationContext(RedisSerializer.string())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<String, BookResponse>(factory, context);
    }

    @Bean(name = "indexRedisTemplate")
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplateIndexes(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(RedisSerializer.string())
                .value(RedisSerializer.string())
                .build();

        return new ReactiveRedisTemplate<String, String>(factory, context);
    }

}
