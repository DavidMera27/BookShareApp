package com.bookshare.wrapper;

import jakarta.validation.constraints.NotBlank;

public record BookDTO(@NotBlank(message = "Title required")
                      String title,

                      @NotBlank(message = "Author required")
                      String author,

                      String description) {
}
