package com.bookshare.controller.handler;

import com.bookshare.document.BookDocument;
import com.bookshare.document.cacher.BookInfo;
import com.bookshare.service.BookService;
import com.bookshare.service.impl.BookCacheService;
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
    private final BookCacheService bookCacheService;
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

    public Mono<ServerResponse> saveBook(ServerRequest serverRequest){
        Mono<BookDTO> book = serverRequest.bodyToMono(BookDTO.class).doOnNext(objectValidator::validate);

        return book.flatMap(bookDTO ->
                bookCacheService.getBook(bookDTO.title())//if cached, transform from Info to DTO and save
                        .doOnNext(a -> System.out.println("obtenido de cache"))
                        .flatMap(info -> bookService.saveBookCached(new BookDTO(info.title(), info.author(), bookDTO.description())))
                        .switchIfEmpty(//if not cached, save it as Info to cache and return DTO
                                bookService.saveBookNoCached(bookDTO)
                                        .doOnNext(a -> System.out.println("obtenido de webclient"))
                                        .flatMap(dto -> bookCacheService.saveBook(dto.title(), new BookInfo(dto.title(), dto.author()))
                                                .thenReturn(dto))
                        )
                        .flatMap(finalBook -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(finalBook)));
    }
}
