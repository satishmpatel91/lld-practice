package com.stackoverflow.service;

import com.stackoverflow.*;
import com.stackoverflow.constants.VoteType;

import java.util.*;
import java.util.stream.Collectors;

public class StackOverflowService {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Question> questions = new LinkedHashMap<>();
    private final Map<String, Post> postsById = new HashMap<>();
    private final ReputationService reputationService = new ReputationService();
    private int idSequence = 0;

    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with id " + user.getId() + " already exists");
        }
        users.put(user.getId(), user);
    }

    public Question askQuestion(String authorId, String title, String body, Set<String> tagNames) {
        User author = requireUser(authorId);
        if (tagNames == null || tagNames.isEmpty()) {
            throw new IllegalArgumentException("Tag set cannot be empty");
        }
        Set<Tag> tags = tagNames.stream().map(Tag::new).collect(Collectors.toSet());
        Question question = new Question(nextId("q"), author, title, body, tags);
        index(question);
        reputationService.onQuestionPosted(author);
        return question;
    }

    public Answer postAnswer(String authorId, String questionId, String body) {
        User author = requireUser(authorId);
        Question question = requireQuestion(questionId);
        Answer answer = new Answer(nextId("a"), author, body);
        question.addAnswer(answer);
        index(answer);
        reputationService.onAnswerPosted(author);
        return answer;
    }

    public Comment addComment(String authorId, String postId, String text) {
        User author = requireUser(authorId);
        Post post = requirePost(postId);
        Comment comment = new Comment(nextId("c"), author, text);
        post.addComment(comment);
        return comment;
    }
    public boolean vote(String voterId, String postId, VoteType type) {
        User user = requireUser(voterId);
        Post post = requirePost(postId);
        if (!post.castVote(user, type)) {
            return false;
        }
        reputationService.onVoteCast(post, type);
        return true;
    }

    public List<Question> searchByKeyword(String keyword) {
        Objects.requireNonNull(keyword, "keyword cannot be null");

        if (keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("keyword cannot be blank");
        }
        return questions.values().stream().filter(q -> q.matchesKeyword(keyword)).toList();

    }

    public List<Question> searchByTag(String tagName) {
        Tag tag = new Tag(tagName);
        return questions.values().stream().filter(q -> q.hasTag(tag)).toList();
    }

    public List<Question> searchByAuthor(String authorId) {
        requireUser(authorId);
        return questions.values().stream()
                .filter(q -> q.getAuthor().getId().equals(authorId))
                .toList();
    }

    public int reputationOf(String userId) {
        User user = requireUser(userId);
        return user.getReputation();
    }

    private User requireUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        return user;
    }

    private Post requirePost(String postId) {
        Objects.requireNonNull(postId, "postId cannot be null");
        Post post = postsById.get(postId);
        if (post == null) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        return post;
    }

    private Question requireQuestion(String questionId) {
        Objects.requireNonNull(questionId, "questionId cannot be null");
        Question question = questions.get(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question not found: " + questionId);
        }
        return question;
    }

    private void index(Question question) {
        questions.put(question.getId(), question);
        postsById.put(question.getId(), question);
    }

    private void index(Answer answer) {
        postsById.put(answer.getId(), answer);
    }

    private String nextId(String prefix) {
        idSequence++;
        return prefix + idSequence;
    }
}
