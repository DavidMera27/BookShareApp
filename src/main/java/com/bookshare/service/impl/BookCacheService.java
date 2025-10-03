package com.bookshare.service.impl;

import com.bookshare.document.cacheable.BookCache;
import com.bookshare.utils.BookRedisTemplateProvider;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static com.bookshare.utils.ServiceUtils.extractKeywords;

@Service
public class BookCacheService {

    private final ReactiveRedisTemplate<String, BookCache> redisTemplateBook;

    private final ReactiveRedisTemplate<String, String> redisTemplateIndexes;

    public BookCacheService(BookRedisTemplateProvider redisProvider) {
        this.redisTemplateBook = redisProvider.getTemplateBooks();
        this.redisTemplateIndexes = redisProvider.getTemplateIndexes();
    }

    public Flux<BookCache> getBooksByTitle(String userInput) {
        List<String> keywords = extractKeywords(userInput); // normalize and split into words
        List<String> redisKeys = keywords.stream()
                .map(word -> "idx:" + word)
                .toList();

        return redisTemplateIndexes.opsForSet()
                .intersect(redisKeys)
                .take(10)
                .flatMap(bookId -> redisTemplateBook.opsForValue().get(bookId));
    }

    public Mono<BookCache> getBooksById(String webBookId) {

        return redisTemplateBook.opsForValue().get(webBookId);
    }

    public Mono<Boolean> saveBook(BookCache book) {
        List<String> keywords = extractKeywords(book.title());
        String bookId = book.id();
        Mono<Boolean> saveMain = redisTemplateBook.opsForValue().set(bookId, book, Duration.ofHours(1));

        List<Mono<Boolean>> indexOps = keywords.stream()
                .map(word -> {
                    String key = "idx:" + word;
                    return redisTemplateIndexes.opsForSet().add(key, bookId)
                            .then(redisTemplateIndexes.expire(key, Duration.ofHours(1)));
                })
                .toList();

        return Mono.zip(saveMain, Mono.when(indexOps)).thenReturn(true);
    }


}
