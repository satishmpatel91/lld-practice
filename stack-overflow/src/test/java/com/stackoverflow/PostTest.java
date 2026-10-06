package com.stackoverflow;

import com.stackoverflow.constants.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostTest {

    private User author;
    private User voter;
    private Post post;

    @BeforeEach
    void setUp() {
        author = new User("u1", "Alice");
        voter = new User("u2", "Bob");
        post = new Answer("a1", author, "Use LinkedHashMap.");
    }

    @Test
    void anUnvotedPostScoresZero() {
        assertEquals(0, post.score());
    }

    @Test
    void anUpvoteCounts() {
        assertTrue(post.castVote(voter, VoteType.UPVOTE));
        assertEquals(1, post.score());
    }

    @Test
    void aDownvoteSubtracts() {
        assertTrue(post.castVote(voter, VoteType.DOWNVOTE));
        assertEquals(-1, post.score());
    }

    @Test
    void upvotesAndDownvotesCancel() {
        assertTrue(post.castVote(voter, VoteType.UPVOTE));
        assertTrue(post.castVote(new User("u3", "Carol"), VoteType.DOWNVOTE));
        assertEquals(0, post.score());
    }

    @Test
    void theSameUserCannotVoteTwice() {
        assertTrue(post.castVote(voter, VoteType.UPVOTE));
        assertFalse(post.castVote(voter, VoteType.UPVOTE));
        assertEquals(1, post.score());
    }

    @Test
    void aSecondVoteDoesNotChangeTheDirection() {
        assertTrue(post.castVote(voter, VoteType.UPVOTE));
        assertFalse(post.castVote(voter, VoteType.DOWNVOTE));
        assertEquals(1, post.score());
    }

    @Test
    void theAuthorCannotVoteOnTheirOwnPost() {
        assertFalse(post.castVote(author, VoteType.UPVOTE));
        assertEquals(0, post.score());
    }

    @Test
    void commentsAreKeptInOrder() {
        post.addComment(new Comment("c1", voter, "first"));
        post.addComment(new Comment("c2", voter, "second"));
        assertEquals(2, post.getComments().size());
        assertEquals("first", post.getComments().get(0).getText());
    }

    @Test
    void theCommentListHandedOutCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> post.getComments().add(new Comment("c1", voter, "sneaky")));
    }
}
