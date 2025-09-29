package com.bookshare.wrapper.response;

public record BookResponse(String id,
                           String title,
                           String author,
                           String issuer) {
}
