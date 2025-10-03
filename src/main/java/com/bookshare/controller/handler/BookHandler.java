package com.bookshare.controller.handler;

import com.bookshare.document.BookDocument;
import com.bookshare.service.BookService;
import com.bookshare.service.impl.BookCacheService;
import com.bookshare.utils.ObjectValidator;
import com.bookshare.wrapper.request.BookRequest;
import com.bookshare.wrapper.response.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookHandler {

    private final BookService bookService;

    private final BookCacheService bookCacheService;

    private final ObjectValidator objectValidator;

    public Mono<ServerResponse> getAllBooks(ServerRequest serverRequest){
        Flux<BookResponse> books = bookService.findAllBooks();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(books, BookDocument.class);
    }

    public Mono<ServerResponse> getOneById(ServerRequest serverRequest){
        String id = serverRequest.pathVariable("id");
        Mono<BookResponse> book = bookService.getById(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(book, BookRequest.class);
    }

    public Mono<ServerResponse> searchBookInside(ServerRequest serverRequest){//Looks into mongodb
        Optional<String> userInput = serverRequest.queryParam("search");

        return Mono.justOrEmpty(userInput)
                .flatMap(search ->
                bookService.findByTitleInside(search)
                        .switchIfEmpty(bookService.findByAuthorInside(search))
                        .collectList()
                        .flatMap(books -> {
                            if (books.isEmpty()) return ServerResponse.notFound().build();
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Flux.fromIterable(books), BookResponse.class);
                        }))
                .switchIfEmpty(ServerResponse.badRequest().build());
    }

    public Mono<ServerResponse> searchBookOutside(ServerRequest serverRequest){//searches into redis if empty then into google books
        Mono<BookRequest> book = serverRequest.bodyToMono(BookRequest.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookCacheService.getBooksByTitle(bookDTO.title())
                        .switchIfEmpty(bookService.findByTitleOutside(bookDTO)
                                .flatMap(found -> bookCacheService.getBooksById(found.id())
                                        .switchIfEmpty(bookCacheService.saveBook(found).thenReturn(found))
                                        .then(Mono.just(found))))
                        .collectList()
                        .flatMap(books -> {
                            if(books.isEmpty()) return ServerResponse.notFound().build();
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Flux.fromIterable(books), BookResponse.class);
                        })
        );
    }

    public Mono<ServerResponse> searchBookOutsideDeeper(ServerRequest serverRequest){//searches straight into google books
        Mono<BookRequest> book = serverRequest.bodyToMono(BookRequest.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookService.findByTitleOutside(bookDTO)
                        .flatMap(found -> bookCacheService.getBooksById(found.id())
                                .switchIfEmpty(bookCacheService.saveBook(found).thenReturn(found))
                                .then(Mono.just(found)))
                        .collectList()
                        .flatMap(books -> {
                            if(books.isEmpty()) return ServerResponse.notFound().build();
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Flux.fromIterable(books), BookResponse.class);
                        })
        );
    }

    public Mono<ServerResponse> subscribeBook(ServerRequest serverRequest){
        return null;
    }

    public Mono<ServerResponse> saveBook(ServerRequest serverRequest){
        Mono<BookRequest> book = serverRequest.bodyToMono(BookRequest.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookService.saveBook(bookDTO)
                        .flatMap(finalBook -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(finalBook)));
    }

    public Mono<ServerResponse> unsubscribeBook(ServerRequest serverRequest){
        return null;
    }

    public Mono<ServerResponse> deleteBook(ServerRequest serverRequest){
        return null;
    }
}
