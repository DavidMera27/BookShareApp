package com.bookshare.wrapper.request;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(@NotBlank(message = "Title required")
                          String title,

                          @NotBlank(message = "Author required")
                          String author,

                          String issuer) {
}
