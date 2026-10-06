package com.stackoverflow;

import com.stackoverflow.constants.VoteType;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract sealed class Post permits Question, Answer {

    @Getter
    private final String id;
    @Getter
    private final User author;
    @Getter
    private final String body;
    @Getter
    private final Instant createdAt;

    private final Map<String, Vote> votesByUserId = new HashMap<>();
    private final List<Comment> comments = new ArrayList<>();

    protected Post(String id, User author, String body) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public boolean castVote(User voter, VoteType type) {
        if (voter.getId().equals(author.getId())) {
            return false;
        }
        if (votesByUserId.containsKey(voter.getId())) {
            return false;
        }
        votesByUserId.put(voter.getId(), new Vote(voter, type));
        return true;
    }

    public int score() {
        int upvotes = 0;
        int downvotes = 0;
        for (Vote vote : votesByUserId.values()) {
            if (vote.type() == VoteType.UPVOTE) {
                upvotes++;
            } else {
                downvotes++;
            }
        }
        return upvotes - downvotes;
    }

    public void addComment(Comment comment) {
       comments.add(comment);
    }

    public List<Comment> getComments() {
        return List.copyOf(comments);
    }


}
