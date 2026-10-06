package com.stackoverflow.service;

import com.stackoverflow.Answer;
import com.stackoverflow.Post;
import com.stackoverflow.Question;
import com.stackoverflow.User;
import com.stackoverflow.constants.VoteType;

public class ReputationService {

    private static final int QUESTION_POSTED = 1;
    private static final int ANSWER_POSTED = 1;
    private static final int QUESTION_UPVOTED = 5;
    private static final int ANSWER_UPVOTED = 10;
    private static final int DOWNVOTED = -2;

    public void onQuestionPosted(User author) {
        author.addReputation(QUESTION_POSTED);
    }

    public void onAnswerPosted(User author) {
        author.addReputation(ANSWER_POSTED);
    }

    public void onVoteCast(Post post, VoteType type) {
        if (type == VoteType.DOWNVOTE) {
            post.getAuthor().addReputation(DOWNVOTED);
            return;
        }
        int delta = switch (post) {
            case Question question -> QUESTION_UPVOTED;
            case Answer answer -> ANSWER_UPVOTED;
        };
        post.getAuthor().addReputation(delta);
    }
}
