package com.bookshare.service;

import com.bookshare.document.BookDocument;
import com.bookshare.wrapper.BookDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookService {
    Flux<BookDTO> findAllBooks();

    Mono<BookDTO> getById(String id);

    Mono<BookDTO> saveBookNoCached(BookDTO book);

    Mono<BookDTO> saveBookCached(BookDTO book);

    Mono<BookDTO> updateBook(String id, BookDTO book);

    Mono<Void> deleteBook(String id);
}
