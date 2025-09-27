package com.bookshare.repository;

import com.bookshare.document.BookDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface BookRepository extends ReactiveMongoRepository<BookDocument, String> {
    Mono<BookDocument> findByTitle(String title);

    Mono<BookDocument> findByAuthor(String author);
}
