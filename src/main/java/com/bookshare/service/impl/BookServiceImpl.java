package com.bookshare.service.impl;

import com.bookshare.document.BookDocument;
import com.bookshare.exception.BookShareException;
import com.bookshare.repository.BookRepository;
import com.bookshare.service.BookService;
import com.bookshare.wrapper.request.BookRequest;
import com.bookshare.wrapper.response.BookResponse;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final static String NOT_FOUND_MESSAGE = "Book not found. BookShare API message.";

    @Value("${book-share.api-key}")
    private String BOOK_SHARE_KEY;

    private final BookRepository bookRepository;

    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<BookResponse> findAllBooks() {
        return bookRepository.findAll()
                .map(this::toDTO);
    }

    @Override
    public Mono<BookResponse> getById(String id) {
        return bookRepository.findById(id)
                .map(this::toDTO)
                .switchIfEmpty(Mono.error(new BookShareException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE)));
    }

    @Override
    public Flux<BookResponse> findByTitle(String input){
        return bookRepository.findByTitleContainingIgnoreCase(input)
                .map(this::toDTO);
    }

    @Override
    public Flux<BookResponse> searchBookInside(BookRequest bookDTO) {
        return null;
    }

    @Override
    public Flux<BookResponse> searchBookOutside(BookRequest book) {
        return null;
    }

    @Override
    public Mono<BookRequest> subscribeBookCached(BookRequest book) {
//        return bookRepository.save(toDocument(book))
//                .map(this::toDTO)
//                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()));
        return null;
    }

    @Override
    public Mono<BookResponse> saveBookNoCached(BookRequest bookDTO) {
        return searchBookValidity(bookDTO.title(), bookDTO.author())
                .flatMap(webBook -> Mono.just(new BookDocument())
                        .doOnNext(newBook -> {
                            newBook.setTitle((webBook.get("title").split("[*&%$#@./;|\\-_()\\[\\]]")[0].trim()));
                            newBook.setAuthor(webBook.get("author"));
                            newBook.setIssuer(bookDTO.issuer());
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
    public Mono<BookResponse> updateBook(String id, BookRequest dto) {
//        return bookRepository.findById(id)
//                .flatMap(book -> {
//                    book.setDescription(dto.description());
//                    return bookRepository.save(book);
//                })
//                .map(this::toDTO);
        return null;
    }

    @Override
    public Mono<Void> deleteBook(String id) {
        return null;
    }



    //#######################UTILITY-METHODS#######################
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
                            if(!volInfo.has("title") || !volInfo.has("authors")){return;}
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

    private BookDocument toDocument(BookRequest book){
        BookDocument bookDocument = new BookDocument();
        bookDocument.setTitle(book.title());
        bookDocument.setAuthor(book.author());
        return bookDocument;
    }

    private BookResponse toDTO(BookDocument book){
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIssuer());
    }
}
