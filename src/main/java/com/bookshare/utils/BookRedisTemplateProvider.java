package com.bookshare.utils;

import com.bookshare.document.cacheable.BookCache;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class BookRedisTemplateProvider {
    private final ReactiveRedisTemplate<String, BookCache> redisTemplateBooks;
    private final ReactiveRedisTemplate<String, String> redisTemplateIndexes;

    public BookRedisTemplateProvider(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<BookCache> serializer = new Jackson2JsonRedisSerializer<>(BookCache.class);
        RedisSerializationContext<String, BookCache> contextBooks = RedisSerializationContext
                .<String, BookCache>newSerializationContext(RedisSerializer.string())
                .value(serializer)
                .build();

        RedisSerializationContext<String, String> contextIndexes = RedisSerializationContext
                .<String, String>newSerializationContext(RedisSerializer.string())
                .value(RedisSerializer.string())
                .build();

        this.redisTemplateBooks = new ReactiveRedisTemplate<>(factory, contextBooks);
        this.redisTemplateIndexes = new ReactiveRedisTemplate<>(factory, contextIndexes);
    }

    public ReactiveRedisTemplate<String, BookCache> getTemplateBooks() {
        return redisTemplateBooks;
    }

    public ReactiveRedisTemplate<String, String> getTemplateIndexes() {
        return redisTemplateIndexes;
    }
}
