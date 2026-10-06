package com.stackoverflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class User {

    private final String id;
    private final String displayName;
    private int reputation;
    public void addReputation(int delta) {
       reputation += delta;
    }
}
