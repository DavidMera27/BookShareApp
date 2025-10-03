package com.bookshare.service;

import com.bookshare.document.cacheable.BookCache;
import com.bookshare.wrapper.request.BookRequest;
import com.bookshare.wrapper.response.BookResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookService {
    Flux<BookResponse> findAllBooks();

    Mono<BookResponse> getById(String id);

    Flux<BookResponse> findByTitleInside(String title);

    Flux<BookResponse> findByAuthorInside(String author);

    Flux<BookCache> findByTitleOutside(BookRequest bookDTO);

    Mono<BookResponse> subscribeBook(String subscriber, String bookId);

    Mono<BookResponse> saveBook(BookRequest bookDTO);

    Mono<Void> unsubscribeBook(String subscriber, String bookId);

    Mono<Void> deleteBook(String id);
}
