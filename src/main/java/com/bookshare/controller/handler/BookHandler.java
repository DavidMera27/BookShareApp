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

    public Mono<ServerResponse> getOne(ServerRequest serverRequest){
        String id = serverRequest.pathVariable("id");
        Mono<BookResponse> book = bookService.getById(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(book, BookRequest.class);
    }

    public Mono<ServerResponse> searchBookInside(ServerRequest serverRequest){
        Mono<BookRequest> book = serverRequest.bodyToMono(BookRequest.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookCacheService.getBooks(bookDTO.title())
                        .switchIfEmpty(bookService.findByTitle(bookDTO.title()))
                        .collectList()
                        .flatMap(books -> {
                            if (books.isEmpty()) {
                                return ServerResponse.notFound().build();
                            }
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Flux.fromIterable(books), BookResponse.class);
                        })
        );
    }

    public Mono<ServerResponse> searchBookOutside(ServerRequest serverRequest){
        return null;
    }

    public Mono<ServerResponse> saveBook(ServerRequest serverRequest){
        Mono<BookRequest> book = serverRequest.bodyToMono(BookRequest.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookService.saveBookNoCached(bookDTO)
                        .flatMap(dto -> bookCacheService.saveBook(dto.title(), dto)
                                .thenReturn(dto))
                        .flatMap(finalBook -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(finalBook)));
    }

    public Mono<ServerResponse> updateBook(ServerRequest serverRequest){
        String id = serverRequest.pathVariable("id");
        Mono<BookRequest> bookDesc = serverRequest.bodyToMono(BookRequest.class);
        return bookDesc
                .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(bookService.updateBook(id, dto), BookRequest.class));
    }
}
