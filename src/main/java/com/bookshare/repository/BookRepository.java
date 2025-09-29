package com.bookshare.repository;

import com.bookshare.document.BookDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookRepository extends ReactiveMongoRepository<BookDocument, String> {
    Flux<BookDocument> findByTitleContainingIgnoreCase(String title);//findByTituloRegexIgnoreCase(".*coronel.*escriba.*");

    Mono<BookDocument> findByAuthor(String author);
}
