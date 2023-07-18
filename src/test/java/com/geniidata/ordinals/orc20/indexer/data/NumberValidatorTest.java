package com.geniidata.ordinals.orc20.indexer.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberValidatorTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void intFromString() {
        String[] invalidNumbers = {"", "+0.1", "1.", ".1", ".", "1.1.1", "1 1", "1. 1", "1.1"};
        for (String invalidNumber : invalidNumbers) {
            assertThrows(Exception.class, () -> NumberValidator.intFromString(invalidNumber));
        }
    }

    @Test
    void longFromString() {
        String[] invalidNumbers = {"", "+0.1", "1.", ".1", ".", "1.1.1", "1 1", "1. 1", "1.1"};
        for (String invalidNumber : invalidNumbers) {
            assertThrows(Exception.class, () -> NumberValidator.longFromString(invalidNumber));
        }
    }

    @Test
    void decimalFromString() {
        String[] invalidNumbers = {"", "+0.1", "1.", ".1", ".", "1.1.1", "1 1", "1. 1"};
        for (String invalidNumber : invalidNumbers) {
            assertThrows(Exception.class, () -> NumberValidator.decimalFromString(invalidNumber));
        }
        assert NumberValidator.decimalFromString("1").scale() == 0;
        assert NumberValidator.decimalFromString("1.1234567").scale() == 7;
        assert NumberValidator.decimalFromString("1.1234567000").scale() == 10;

    }
}