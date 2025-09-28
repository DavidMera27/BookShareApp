package com.bookshare.service.impl;

import com.bookshare.document.BookDocument;
import com.bookshare.exception.BookShareException;
import com.bookshare.repository.BookRepository;
import com.bookshare.service.BookService;
import com.bookshare.wrapper.BookDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final static String NOT_FOUND_MESSAGE = "Book not found. BookShare API message.";

    @Value("${book-share.api-key}")
    private String BOOK_SHARE_KEY;

    private final BookRepository bookRepository;

    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<BookDTO> findAllBooks() {
        return bookRepository.findAll()
                .map(this::toDTO);
    }

    @Override
    public Mono<BookDTO> getById(String id) {
        return bookRepository.findById(id)
                .map(this::toDTO)
                .switchIfEmpty(Mono.error(new BookShareException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE)));
    }

    @Override
    public Mono<BookDTO> saveBookNoCached(BookDTO bookDTO) {
        return searchBookValidity(bookDTO.title(), bookDTO.author())
                .flatMap(webBook -> Mono.just(new BookDocument())
                        .doOnNext(newBook -> {
                            newBook.setTitle(webBook.get("title"));
                            newBook.setAuthor(webBook.get("author"));
                            newBook.setDescription(bookDTO.description());
                        })
                        .flatMap(bookRepository::save)
                        .map(this::toDTO))
                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()))
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(5))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new BookShareException(HttpStatus.BAD_REQUEST, NOT_FOUND_MESSAGE)
                        )
                );
    }

    @Override
    public Mono<BookDTO> saveBookCached(BookDTO book) {
        return bookRepository.save(toDocument(book))
                .map(this::toDTO)
                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()));
    }

    @Override
    public Mono<BookDTO> updateBook(String id, BookDTO dto) {
        return bookRepository.findById(id)
                .flatMap(book -> {
                    book.setDescription(dto.description());
                    return bookRepository.save(book);
                })
                .map(this::toDTO);
//        return bookRepository.save(toDocument(book))
//                .map(this::toDTO);
    }

    @Override
    public Mono<Void> deleteBook(String id) {
        return null;
    }

    public Mono<Map<String, String>> searchBookValidity(String title, String author){
        String query = String.format("q=intitle:%s+inauthor:%s&maxResults=5&key=%s", title, author, BOOK_SHARE_KEY);
        WebClient webClientOpenBook = webClientBuilder.baseUrl("https://www.googleapis.com/books/v1").build();
        Map<String, String> result = new HashMap<>();

        return webClientOpenBook.get()
                .uri("/volumes?" + query)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    JsonNode items = json.get("items");
                    if (items != null && items.isArray() && !items.isEmpty()) {
                        JsonNode volumeInfo = items.get(0).get("volumeInfo");
                        String tituloEncontrado = volumeInfo.get("title").asText();
                        String autorEncontrado = volumeInfo.get("authors").get(0).asText();
                        result.put("title", tituloEncontrado);
                        result.put("author", autorEncontrado);
                        //For logs only
                        items.forEach(i -> {
                            JsonNode volInfo = i.get("volumeInfo");
                            JsonNode imageLinks = volInfo.get("imageLinks");
                            String smallThumbnail = imageLinks != null && imageLinks.has("smallThumbnail")
                                    ? imageLinks.get("smallThumbnail").asText()
                                    : null;
                            String thumbnail = imageLinks != null && imageLinks.has("thumbnail")
                                    ? imageLinks.get("thumbnail").asText()
                                    : null;
                            System.out.println(volInfo.get("title").asText() + " " +
                                    volInfo.get("authors").get(0).asText() + " " +
                                    smallThumbnail + " " +
                                    thumbnail);});
                        //...
                        return result;
                    } else {
                        throw new BookShareException(HttpStatus.BAD_REQUEST, "NOT_FOUND_MESSAGE");
                    }
                });
    }

    private BookDocument toDocument(BookDTO book){
        BookDocument bookDocument = new BookDocument();
        bookDocument.setTitle(book.title());
        bookDocument.setAuthor(book.author());
        bookDocument.setDescription(book.description());
        return bookDocument;
    }

    private BookDTO toDTO(BookDocument book){
        return new BookDTO(book.getTitle(), book.getAuthor(), book.getDescription());
    }
}
