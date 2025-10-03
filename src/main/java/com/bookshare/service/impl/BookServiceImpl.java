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

import static com.bookshare.utils.ServiceUtils.normalizeTitle;
import static com.bookshare.utils.ServiceUtils.printBooks;


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
    public Flux<BookResponse> findByTitleInside(String input){
        return bookRepository.findByTitleContainingIgnoreCase(normalizeTitle(input))
                .map(this::toDTO);
    }

    @Override
    public Flux<BookResponse> findByTitleOutside(BookRequest bookDTO) {
        return searchBookValidity(bookDTO.title(), bookDTO.author())
                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()))
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(5))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new BookShareException(HttpStatus.BAD_REQUEST, NOT_FOUND_MESSAGE)
                        )
                );
    }

    @Override
    public Mono<BookResponse> subscribeBook(String subscriber, String bookId) {
        return null;
    }

    @Override
    public Mono<BookResponse> saveBook(BookRequest bookDTO) {
        return bookRepository.save(this.toDocument(bookDTO))
                .map(this::toDTO)
                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()));
    }

    @Override
    public Mono<BookResponse> unsubscribeBook(String subscriber, String bookId) {
        return null;
    }

    @Override
    public Mono<Void> deleteBook(String id) {
        return null;
    }


    //<--Complements
    public Flux<BookResponse> searchBookValidity(String title, String author){
        String query = String.format("q=intitle:%s+inauthor:%s&maxResults=5&key=%s", title, author, BOOK_SHARE_KEY);
        WebClient webClientOpenBook = webClientBuilder.baseUrl("https://www.googleapis.com/books/v1").build();

        return webClientOpenBook.get()
                .uri("/volumes?" + query)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(json -> {
                    JsonNode items = json.get("items");
                    if (items == null || !items.isArray() || items.isEmpty()) throw new BookShareException(HttpStatus.BAD_REQUEST, NOT_FOUND_MESSAGE);
                    printBooks(items);
                    return Flux.fromIterable(items);
                })
                .flatMap(jsonNode -> {
                    JsonNode volumeInfo = jsonNode.get("volumeInfo");
                    if(volumeInfo == null || !volumeInfo.has("title") || !volumeInfo.has("authors")) return Mono.empty();
                    JsonNode bookImageLinks =  volumeInfo.has("imageLinks")
                            ? volumeInfo.get("imageLinks")
                            : null;
                    String bookThumbnail = bookImageLinks != null && bookImageLinks.has("thumbnail")
                            ? bookImageLinks.get("thumbnail").asText()
                            : null;
                    String foundTitle = volumeInfo.get("title").asText();
                    String foundAuthor = volumeInfo.get("authors").get(0).asText();
                    return Mono.just(new BookResponse(null, normalizeTitle(foundTitle), foundAuthor, null, bookThumbnail, null, null));
                });
    }

    private BookDocument toDocument(BookRequest book){
        BookDocument bookDocument = new BookDocument();
        bookDocument.setTitle(normalizeTitle(book.title()));;
        bookDocument.setAuthor(book.author());
        bookDocument.setIssuer(book.issuer());
        bookDocument.setImage(book.image());
        return bookDocument;
    }

    private BookResponse toDTO(BookDocument book){
        return new BookResponse(book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIssuer(),
                book.getImage(),
                book.getCreatedAt().toString(),
                book.getModifiedAt().toString());
    }
    //Complements-->
}
