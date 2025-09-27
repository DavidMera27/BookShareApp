package com.bookshare.controller.handler;

import com.bookshare.document.BookDocument;
import com.bookshare.service.BookService;
import com.bookshare.utils.ObjectValidator;
import com.bookshare.wrapper.BookDTO;
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
    private final ObjectValidator objectValidator;

    public Mono<ServerResponse> getAllBooks(ServerRequest serverRequest){
        Flux<BookDTO> books = bookService.findAllBooks();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(books, BookDocument.class);
    }

    public Mono<ServerResponse> getOne(ServerRequest serverRequest){
        String id = serverRequest.pathVariable("id");
        Mono<BookDTO> book = bookService.getById(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(book, BookDTO.class);
    }

    public Mono<ServerResponse> saveProduct(ServerRequest serverRequest){
        Mono<BookDTO> book = serverRequest.bodyToMono(BookDTO.class).doOnNext(objectValidator::validate);
        return book.flatMap(b -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(bookService.saveBook(b), BookDTO.class));
    }
}
