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

import java.text.Normalizer;
import java.time.Duration;

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
        return bookRepository.findByTitleContainingIgnoreCase(input)
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
    public Mono<BookRequest> subscribeBookCached(BookRequest book) {
//        return bookRepository.save(toDocument(book))
//                .map(this::toDTO)
//                .onErrorMap(err -> new BookShareException(HttpStatus.BAD_REQUEST, err.getMessage()));
        return null;
    }

    @Override
    public Mono<BookResponse> saveBook(BookRequest bookDTO) {
        return bookRepository.save(this.toDocument(bookDTO))
                .map(this::toDTO)
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
    public Flux<BookResponse> searchBookValidity(String title, String author){
        String query = String.format("q=intitle:%s+inauthor:%s&maxResults=5&key=%s", title, author, BOOK_SHARE_KEY);
        WebClient webClientOpenBook = webClientBuilder.baseUrl("https://www.googleapis.com/books/v1").build();

        return webClientOpenBook.get()
                .uri("/volumes?" + query)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(json -> {
                    JsonNode items = json.get("items");
                    printBooks(items);
                    if (items != null && items.isArray() && !items.isEmpty()) {
                        return Flux.fromIterable(items);
                    } else {
                        throw new BookShareException(HttpStatus.BAD_REQUEST, "NOT_FOUND_MESSAGE");
                    }
                })
                .flatMap(jsonNode -> {
                    JsonNode volumeInfo = jsonNode.get("volumeInfo");
                    if(volumeInfo == null || !volumeInfo.has("title") || !volumeInfo.has("authors")) return Mono.empty();
                    String foundTitle = volumeInfo.get("title").asText();
                    String foundAuthor = volumeInfo.get("authors").get(0).asText();
                    return Mono.just(new BookResponse(null, normalizeTitle(foundTitle), foundAuthor, null));
                });
    }

    private void printBooks(JsonNode items){//for logs only
        items.forEach(i -> {
            JsonNode volInfo = i.get("volumeInfo");
            if(volInfo == null || !volInfo.has("title") || !volInfo.has("authors")){return;}
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
    }

    public static String normalizeTitle(String title) {
        if (title == null || title.isEmpty()) return "";

        String cleaned = title.split("[*&%$#@./;|\\-_()\\[\\]]")[0].trim();

        String normalized = Normalizer.normalize(cleaned, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return normalized;
    }

    private BookDocument toDocument(BookRequest book){
        BookDocument bookDocument = new BookDocument();
        bookDocument.setTitle(normalizeTitle(book.title()));;
        bookDocument.setAuthor(book.author());
        bookDocument.setIssuer(book.issuer());
        return bookDocument;
    }

    private BookResponse toDTO(BookDocument book){
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIssuer());
    }
}
