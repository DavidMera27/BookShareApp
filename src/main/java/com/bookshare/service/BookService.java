package com.bookshare.service;

import com.bookshare.wrapper.request.BookRequest;
import com.bookshare.wrapper.response.BookResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookService {
    Flux<BookResponse> findAllBooks();

    Mono<BookResponse> getById(String id);

    Flux<BookResponse> findByTitleInside(String input);

    Flux<BookResponse> findByTitleOutside(BookRequest bookDTO);

    Mono<BookResponse> subscribeBook(String subscriber, String bookId);

    Mono<BookResponse> saveBook(BookRequest bookDTO);

    Mono<Void> deleteBook(String id);
}
