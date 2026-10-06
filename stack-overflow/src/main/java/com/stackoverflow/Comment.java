package com.stackoverflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
@Getter
public class Comment {

    private final String id;
    private final User author;
    private final String text;
    private final Instant createdAt;

    public Comment(String id, User author, String text) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.createdAt = Instant.now();
    }

}
