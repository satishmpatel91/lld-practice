package com.stackoverflow;

import com.stackoverflow.constants.VoteType;

public record Vote(User voter, VoteType type) {
}
