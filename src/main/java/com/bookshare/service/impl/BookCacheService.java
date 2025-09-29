package com.bookshare.service.impl;

import com.bookshare.wrapper.response.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
public class BookCacheService {

    private final ReactiveRedisTemplate<String, BookResponse> redisTemplateBook;

    private final ReactiveRedisTemplate<String, String> redisTemplateIndexes;

    public BookCacheService(
            @Qualifier("bookRedisTemplate") ReactiveRedisTemplate<String, BookResponse> bookRedisTemplate,
            @Qualifier("indexRedisTemplate") ReactiveRedisTemplate<String, String> indexRedisTemplate
    ) {
        this.redisTemplateBook = bookRedisTemplate;
        this.redisTemplateIndexes = indexRedisTemplate;
    }

    public Flux<BookResponse> getBooks(String userInput) {
        List<String> keywords = extractKeywords(userInput); // normalize and split into words

        List<String> redisKeys = keywords.stream()
                .map(word -> "idx:" + word)
                .toList();

        return redisTemplateIndexes.opsForSet()
                .intersect(redisKeys) // returns Mono<Set<String>> with the IDs
//                .flatMapMany(Flux::fromIterable)
                .take(10) // limit to first 10
                .flatMap(bookId -> redisTemplateBook.opsForValue().get(bookId)); // gets every book
    }

    public Mono<Boolean> saveBook(String title, BookResponse book) {
        List<String> keywords = extractKeywords(title);
        String bookId = book.id();

        Mono<Boolean> saveMain = redisTemplateBook.opsForValue().set(bookId, book, Duration.ofHours(1));

        List<Mono<Long>> indexOps = keywords.stream()
                .map(word -> redisTemplateIndexes.opsForSet().add("idx:" + word, bookId))
                .toList();

        return Mono.zip(saveMain, Mono.when(indexOps)).thenReturn(true);
    }

    public List<String> extractKeywords(String title) {
        String noAccent = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();

        String[] words = noAccent.split("\\s+");
        return Arrays.stream(words)
                .limit(7)
                .toList();
    }


//    public Mono<BookRequest> getBook(String key) {
//        return redisTemplate.opsForValue().get(this.normalizeTitle(key));
//    }
//
//    public Mono<Boolean> saveBook(String key, BookRequest book) {
//        return redisTemplate.opsForValue().set(this.normalizeTitle(key), book, Duration.ofHours(1));
//    }
//
//    public String normalizeTitle(String title) {
//        String noAccent = Normalizer.normalize(title, Normalizer.Form.NFD)
//                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
//        return noAccent.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
//    }
}
