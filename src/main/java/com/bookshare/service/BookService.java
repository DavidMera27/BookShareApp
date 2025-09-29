package com.bookshare.service;

import com.bookshare.wrapper.request.BookRequest;
import com.bookshare.wrapper.response.BookResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookService {
    Flux<BookResponse> findAllBooks();

    Mono<BookResponse> getById(String id);

    Flux<BookResponse> findByTitle(String input);

    Flux<BookResponse> searchBookInside(BookRequest bookDTO);

    Flux<BookResponse> searchBookOutside(BookRequest bookDTO);

    Mono<BookRequest> subscribeBookCached(BookRequest bookDTO);

    Mono<BookResponse> saveBookNoCached(BookRequest bookDTO);

    Mono<BookResponse> updateBook(String id, BookRequest bookDTO);

    Mono<Void> deleteBook(String id);
}
