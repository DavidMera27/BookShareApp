package com.bookshare.wrapper.response;

import java.time.LocalDate;

public record BookResponse(String id,
                           String title,
                           String author,
                           String issuer,
                           String image,
                           String createdAt,
                           String modifiedAt) {
}
