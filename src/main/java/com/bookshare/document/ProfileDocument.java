package com.bookshare.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "profiles")
public class ProfileDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @CreatedDate
    @Field("joined_at")
    private LocalDate joinedAt;

//    private String userId;
}
