package com.bookshare.utils;

import com.bookshare.wrapper.response.BookResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class BookRedisTemplateProvider {
    private final ReactiveRedisTemplate<String, BookResponse> redisTemplateBooks;
    private final ReactiveRedisTemplate<String, String> redisTemplateIndexes;

    public BookRedisTemplateProvider(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<BookResponse> serializer = new Jackson2JsonRedisSerializer<>(BookResponse.class);
        RedisSerializationContext<String, BookResponse> contextBooks = RedisSerializationContext
                .<String, BookResponse>newSerializationContext(RedisSerializer.string())
                .value(serializer)
                .build();

        RedisSerializationContext<String, String> contextIndexes = RedisSerializationContext
                .<String, String>newSerializationContext(RedisSerializer.string())
                .value(RedisSerializer.string())
                .build();

        this.redisTemplateBooks = new ReactiveRedisTemplate<>(factory, contextBooks);
        this.redisTemplateIndexes = new ReactiveRedisTemplate<>(factory, contextIndexes);
    }

    public ReactiveRedisTemplate<String, BookResponse> getTemplateBooks() {
        return redisTemplateBooks;
    }

    public ReactiveRedisTemplate<String, String> getTemplateIndexes() {
        return redisTemplateIndexes;
    }
}
