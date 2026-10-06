package com.stackoverflow.service;

import com.stackoverflow.Answer;
import com.stackoverflow.Question;
import com.stackoverflow.Tag;
import com.stackoverflow.User;
import com.stackoverflow.constants.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReputationServiceTest {

    private ReputationService reputation;
    private User author;

    @BeforeEach
    void setUp() {
        reputation = new ReputationService();
        author = new User("u1", "Alice");
    }

    private Question question() {
        return new Question("q1", author, "title", "body", Set.of(new Tag("java")));
    }

    private Answer answer() {
        return new Answer("a1", author, "body");
    }

    @Test
    void postingAQuestionEarnsOne() {
        reputation.onQuestionPosted(author);
        assertEquals(1, author.getReputation());
    }

    @Test
    void postingAnAnswerEarnsOne() {
        reputation.onAnswerPosted(author);
        assertEquals(1, author.getReputation());
    }

    @Test
    void postingAccruesOnePerPostRatherThanDoubling() {
        reputation.onQuestionPosted(author);
        reputation.onQuestionPosted(author);
        reputation.onQuestionPosted(author);
        assertEquals(3, author.getReputation());
    }

    @Test
    void aQuestionUpvoteEarnsFive() {
        reputation.onVoteCast(question(), VoteType.UPVOTE);
        assertEquals(5, author.getReputation());
    }

    @Test
    void anAnswerUpvoteEarnsTen() {
        reputation.onVoteCast(answer(), VoteType.UPVOTE);
        assertEquals(10, author.getReputation());
    }

    @Test
    void aQuestionDownvoteCostsTwo() {
        reputation.onVoteCast(question(), VoteType.DOWNVOTE);
        assertEquals(-2, author.getReputation());
    }

    @Test
    void anAnswerDownvoteCostsTwo() {
        reputation.onVoteCast(answer(), VoteType.DOWNVOTE);
        assertEquals(-2, author.getReputation());
    }

    @Test
    void reputationIsTheRunningSumOfTheEventsApplied() {
        reputation.onQuestionPosted(author);
        reputation.onVoteCast(question(), VoteType.UPVOTE);
        reputation.onVoteCast(answer(), VoteType.UPVOTE);
        reputation.onVoteCast(question(), VoteType.DOWNVOTE);
        assertEquals(1 + 5 + 10 - 2, author.getReputation());
    }
}
