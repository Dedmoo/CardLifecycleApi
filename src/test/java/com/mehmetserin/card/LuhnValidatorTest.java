package com.mehmetserin.card;

import com.mehmetserin.card.service.LuhnValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuhnValidatorTest {

    @Test
    void knownValidNumbers_pass() {
        assertTrue(LuhnValidator.isValid("4111111111111111"));
        assertTrue(LuhnValidator.isValid("5555555555554444"));
    }

    @Test
    void invalidNumber_fails() {
        assertFalse(LuhnValidator.isValid("4111111111111112"));
        assertFalse(LuhnValidator.isValid("1234567890123456"));
    }

    @Test
    void nonDigits_fail() {
        assertFalse(LuhnValidator.isValid("abcd"));
        assertFalse(LuhnValidator.isValid(null));
    }

    @Test
    void checkDigit_makesNumberValid() {
        String partial = "411111111111111";
        int check = LuhnValidator.checkDigit(partial);
        assertTrue(LuhnValidator.isValid(partial + check));
        assertEquals(1, check);
    }
}
