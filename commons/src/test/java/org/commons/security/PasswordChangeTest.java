package org.commons.security;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordChangeTest {

    // Stand-in for a PasswordEncoder: the "hash" is the password with a prefix.
    private static final BiPredicate<String, String> MATCHES = (raw, hash) -> hash.equals("h:" + raw);

    @Test
    void validChangeIsAccepted() {
        assertNull(PasswordChange.problem("h:Old-Pass-1", "Old-Pass-1", "New-Pass-22", "New-Pass-22", MATCHES));
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        assertEquals("Your current password is not correct.",
                PasswordChange.problem("h:Old-Pass-1", "guess", "New-Pass-22", "New-Pass-22", MATCHES));
    }

    @Test
    void shortMismatchedOrUnchangedPasswordsAreRejected() {
        assertNotNull(PasswordChange.problem("h:Old-Pass-1", "Old-Pass-1", "short", "short", MATCHES));
        assertNotNull(PasswordChange.problem("h:Old-Pass-1", "Old-Pass-1", "New-Pass-22", "New-Pass-23", MATCHES));
        assertNotNull(PasswordChange.problem("h:Old-Pass-1", "Old-Pass-1", "Old-Pass-1", "Old-Pass-1", MATCHES));
    }

    @Test
    void socialSignInAccountsHaveNoPassword() {
        assertNotNull(PasswordChange.problem(null, "x", "New-Pass-22", "New-Pass-22", MATCHES));
    }
}
