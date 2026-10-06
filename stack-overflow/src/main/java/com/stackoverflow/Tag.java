package com.stackoverflow;

public record Tag(String name) {

    public Tag {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be blank");
        }
        name = name.trim().toLowerCase();
    }
}
