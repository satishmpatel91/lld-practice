package com.stackoverflow;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public final class Question extends Post {
    @Getter
    private final String title;
    private final Set<Tag> tags;
    private final List<Answer> answers = new ArrayList<>();

    public Question(String id, User author, String title, String body, Set<Tag> tags) {
        super(id, author, body);
        this.title = title;
        this.tags = Set.copyOf(tags);
    }

    public void addAnswer(Answer answer) {
        this.answers.add(answer);
    }

    public boolean hasTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        return tags.contains(tag);
    }

    public boolean matchesKeyword(String keyword) {
        keyword = keyword.toLowerCase();
        if(title.toLowerCase().contains(keyword)) {
            return true;
        } else if (getBody().toLowerCase().contains(keyword)) {
            return true;
        } else {
            return false;
        }
    }

    public Set<Tag> getTags() {
        return Set.copyOf(tags);
    }

    public List<Answer> getAnswers() {
        return List.copyOf(answers);
    }
}
