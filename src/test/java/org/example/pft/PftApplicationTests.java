package org.example.pft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PftApplicationTests {

    @Test
    void applicationClassLoads() {
        assertDoesNotThrow(() -> Class.forName(PftApplication.class.getName()));
    }

}
