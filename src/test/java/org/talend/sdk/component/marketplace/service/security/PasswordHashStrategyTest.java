package org.talend.sdk.component.marketplace.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MonoMeecrowaveConfig
class PasswordHashStrategyTest {
    @Inject
    private PasswordHashStrategy strategy;

    @Test
    void hash() {
        assertEquals(
                "SHA-512$$8$$049F81509FB0DC31CF29186C8BA157702D4C89AF49F78B9048CF5290EA9A1A8F632D1AEB6ABC7DE9C247945BCE12C6FAD9AC5FD7ED0DE47458912A1D2D7779B3",
                strategy.hash("sUp3rS3cre7"));
    }

    @Test
    void compare() {
        final String actualPassword = "sUp3rS3cre7";
        final String hashed = strategy.hash(actualPassword);
        assertTrue(strategy.compare(hashed, actualPassword));
        assertFalse(strategy.compare(hashed, actualPassword + "wrong"));
    }
}
