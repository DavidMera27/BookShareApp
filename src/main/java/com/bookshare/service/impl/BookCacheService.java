package com.bookshare.service.impl;

import com.bookshare.document.cacher.BookInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BookCacheService {

    private final ReactiveRedisTemplate<String, BookInfo> redisTemplate;

    public Mono<BookInfo> getBook(String key) {
        return redisTemplate.opsForValue().get(this.normalizeTitle(key));
    }

    public Mono<Boolean> saveBook(String key, BookInfo book) {
        return redisTemplate.opsForValue().set(this.normalizeTitle(key), book, Duration.ofHours(1));
    }

    public String normalizeTitle(String title) {
        String noAccent = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccent.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }
}
