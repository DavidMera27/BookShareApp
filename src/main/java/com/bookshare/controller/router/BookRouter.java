package com.bookshare.controller.router;

import com.bookshare.controller.handler.BookHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class BookRouter {

    public static final String PATH = "api/book";

    @Bean
    RouterFunction<ServerResponse> router(BookHandler handler){
        return RouterFunctions.route()
                .GET(PATH, handler::getAllBooks)
                .GET(PATH + "/{id}", handler::getOneById)
                .POST(PATH + "/inside", handler::searchBookInside)
                .POST(PATH + "/outside", handler::searchBookOutside)
                .POST(PATH + "/outsideMore", handler::searchBookOutsideDeeper)
                .POST(PATH + "/subscribe", handler::subscribeBook)
                .POST(PATH + "/save", handler::saveBook)
                .DELETE(PATH + "unsubscribe", handler::unsubscribeBook)
                .DELETE(PATH + "delete/{id}", handler::deleteBook)
                .build();
    }
}
