package com.stackoverflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagTest {

    @Test
    void lowercasesTheName() {
        assertEquals("java", new Tag("JAVA").name());
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("collections", new Tag("  collections  ").name());
    }

    @Test
    void tagsWithTheSameNormalizedNameAreEqual() {
        assertEquals(new Tag("Java"), new Tag(" java "));
        assertEquals(new Tag("Java").hashCode(), new Tag(" java ").hashCode());
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Tag(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Tag("   "));
    }
}
