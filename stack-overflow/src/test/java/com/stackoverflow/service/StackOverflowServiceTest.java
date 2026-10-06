package com.stackoverflow.service;

import com.stackoverflow.Answer;
import com.stackoverflow.Comment;
import com.stackoverflow.Question;
import com.stackoverflow.Tag;
import com.stackoverflow.User;
import com.stackoverflow.constants.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackOverflowServiceTest {

    private StackOverflowService service;

    @BeforeEach
    void setUp() {
        service = new StackOverflowService();
        service.addUser(new User("u1", "Alice"));
        service.addUser(new User("u2", "Bob"));
        service.addUser(new User("u3", "Carol"));
    }

    private Question askTheHashMapQuestion() {
        return service.askQuestion("u1", "Why is my HashMap unordered?",
                "Iteration order keeps changing.", Set.of("Java", "collections"));
    }

    @Test
    void addingTheSameUserTwiceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addUser(new User("u1", "Alice")));
    }

    @Test
    void askingAQuestionStoresItAndPaysTheAuthor() {
        Question question = askTheHashMapQuestion();
        assertEquals("q1", question.getId());
        assertEquals(1, service.reputationOf("u1"));
    }

    @Test
    void questionTagsAreNormalized() {
        Question question = askTheHashMapQuestion();
        List<String> names = question.getTags().stream().map(Tag::name).sorted().toList();
        assertEquals(List.of("collections", "java"), names);
    }

    @Test
    void aQuestionNeedsAtLeastOneTag() {
        assertThrows(IllegalArgumentException.class,
                () -> service.askQuestion("u1", "title", "body", Set.of()));
    }

    @Test
    void anUnknownAuthorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.askQuestion("nobody", "title", "body", Set.of("java")));
    }

    @Test
    void answeringAttachesTheAnswerAndPaysTheAnswerer() {
        askTheHashMapQuestion();
        Answer answer = service.postAnswer("u2", "q1", "HashMap makes no ordering promise.");
        assertEquals("a2", answer.getId());
        assertEquals(1, service.searchByKeyword("hashmap").get(0).getAnswers().size());
        assertEquals(1, service.reputationOf("u2"));
    }

    @Test
    void answeringAnUnknownQuestionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.postAnswer("u2", "q99", "body"));
    }

    @Test
    void aCommentAttachesToAQuestion() {
        Question question = askTheHashMapQuestion();
        Comment comment = service.addComment("u2", "q1", "Which Java version?");
        assertEquals(1, question.getComments().size());
        assertEquals(comment.getId(), question.getComments().get(0).getId());
    }

    @Test
    void aCommentAttachesToAnAnswer() {
        askTheHashMapQuestion();
        Answer answer = service.postAnswer("u2", "q1", "Use LinkedHashMap.");
        service.addComment("u3", "a2", "That fixed it.");
        assertEquals(1, answer.getComments().size());
    }

    @Test
    void commentingEarnsNothing() {
        askTheHashMapQuestion();
        service.addComment("u3", "q1", "Which Java version?");
        assertEquals(0, service.reputationOf("u3"));
    }

    @Test
    void upvotingAnAnswerPaysItsAuthorTen() {
        askTheHashMapQuestion();
        Answer answer = service.postAnswer("u2", "q1", "Use LinkedHashMap.");
        assertTrue(service.vote("u3", "a2", VoteType.UPVOTE));
        assertEquals(11, service.reputationOf("u2"));
        assertEquals(1, answer.score());
    }

    @Test
    void aRepeatVoteChangesNothing() {
        askTheHashMapQuestion();
        service.postAnswer("u2", "q1", "Use LinkedHashMap.");
        assertTrue(service.vote("u3", "a2", VoteType.UPVOTE));
        assertFalse(service.vote("u3", "a2", VoteType.DOWNVOTE));
        assertEquals(11, service.reputationOf("u2"));
    }

    @Test
    void votingOnYourOwnPostChangesNothing() {
        askTheHashMapQuestion();
        assertFalse(service.vote("u1", "q1", VoteType.UPVOTE));
        assertEquals(1, service.reputationOf("u1"));
    }

    @Test
    void votingOnAnUnknownPostIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.vote("u3", "q99", VoteType.UPVOTE));
    }

    @Test
    void anAnswerIsVotableByItsOwnIdNotOnlyThroughItsQuestion() {
        askTheHashMapQuestion();
        service.postAnswer("u2", "q1", "Use LinkedHashMap.");
        assertTrue(service.vote("u3", "a2", VoteType.UPVOTE));
    }

    @Test
    void searchByKeywordMatchesTheTitleCaseInsensitively() {
        askTheHashMapQuestion();
        assertEquals(1, service.searchByKeyword("HASHMAP").size());
    }

    @Test
    void searchByKeywordMatchesTheBody() {
        askTheHashMapQuestion();
        assertEquals(1, service.searchByKeyword("iteration").size());
    }

    @Test
    void searchByKeywordMissesWhatIsNotThere() {
        askTheHashMapQuestion();
        assertTrue(service.searchByKeyword("kafka").isEmpty());
    }

    @Test
    void searchByKeywordRejectsABlankTerm() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByKeyword("   "));
    }

    @Test
    void searchByTagIgnoresTheCaseOfTheQuery() {
        askTheHashMapQuestion();
        assertEquals(1, service.searchByTag("JAVA").size());
    }

    @Test
    void searchByTagMissesAnUnusedTag() {
        askTheHashMapQuestion();
        assertTrue(service.searchByTag("python").isEmpty());
    }

    @Test
    void searchByAuthorFindsOnlyTheirQuestions() {
        askTheHashMapQuestion();
        assertEquals(1, service.searchByAuthor("u1").size());
        assertTrue(service.searchByAuthor("u2").isEmpty());
    }

    @Test
    void searchByAuthorRejectsAnUnknownUser() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByAuthor("nobody"));
    }

    @Test
    void searchDoesNotReturnAnswersAsQuestions() {
        askTheHashMapQuestion();
        service.postAnswer("u2", "q1", "LinkedHashMap keeps insertion order.");
        assertTrue(service.searchByKeyword("insertion order").isEmpty());
    }

    @Test
    void idsAreUniqueAcrossQuestionsAnswersAndComments() {
        askTheHashMapQuestion();
        Answer answer = service.postAnswer("u2", "q1", "Use LinkedHashMap.");
        Comment comment = service.addComment("u3", "a2", "Thanks.");
        assertEquals("q1", service.searchByAuthor("u1").get(0).getId());
        assertEquals("a2", answer.getId());
        assertEquals("c3", comment.getId());
    }
}
