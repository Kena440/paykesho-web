package org.commons.security;

import java.util.function.BiPredicate;

/**
 * Rules for a signed-in user changing their own password, shared by every portal.
 * The caller supplies how to check a raw password against the stored hash
 * (the portal's PasswordEncoder), so this class has no Spring dependency.
 */
public final class PasswordChange {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72; // bcrypt ignores anything longer

    private PasswordChange() {
    }

    /** Why the change cannot be made, in words the user can act on, or null if it can. */
    public static String problem(String storedHash, String current, String next, String confirm,
                                 BiPredicate<String, String> matches) {
        if (storedHash == null || storedHash.isEmpty()) {
            return "This account signs in with Google or Facebook, so it has no password to change.";
        }
        if (current == null || !matches.test(current, storedHash)) {
            return "Your current password is not correct.";
        }
        if (next == null || next.length() < MIN_LENGTH) {
            return "The new password must be at least " + MIN_LENGTH + " characters.";
        }
        if (next.length() > MAX_LENGTH) {
            return "The new password must be at most " + MAX_LENGTH + " characters.";
        }
        if (!next.equals(confirm)) {
            return "The new password and its confirmation do not match.";
        }
        if (next.equals(current)) {
            return "The new password must be different from the current one.";
        }
        return null;
    }
}
