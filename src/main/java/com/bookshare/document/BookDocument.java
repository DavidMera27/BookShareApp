package com.bookshare.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "books")
@CompoundIndex(name = "unique_title_issuer", def = "{'title': 1, 'issuer': 1}", unique = true)
public class BookDocument {

    @Id
    private String id;

    private String title;
    private String author;

    private String issuer;

    private String image;

    @CreatedDate
    @Field("created_at")
    private LocalDate createdAt;

    @LastModifiedDate
    @Field("modified_at")
    private LocalDate modifiedAt;
}
