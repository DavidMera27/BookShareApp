package com.bookshare.wrapper.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(@NotBlank(message = "Title required")
                          String title,

                          @NotNull(message = "Author required")
                          String author,

                          String image,

                          String issuer) {
}
