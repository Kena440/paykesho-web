package org.paykesho.merchantportal.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple CAPTCHA service for password reset forms to prevent automated attacks
 * 
 * Security Features:
 * - Mathematical CAPTCHA challenges
 * - Session-based challenge storage
 * - Time-limited challenges (5 minutes)
 * - Rate limiting per session
 * - Protection against automated password reset attempts
 */
@Slf4j
@Component
public class CaptchaService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CHALLENGE_TIMEOUT_MINUTES = 5;
    private static final int MAX_ATTEMPTS_PER_SESSION = 3;
    
    // Storage for active challenges
    private final ConcurrentMap<String, CaptchaChallenge> challenges = new ConcurrentHashMap<>();
    
    /**
     * Generate a new CAPTCHA challenge for a session
     * @param sessionId The session identifier
     * @return CaptchaChallenge containing the question and challenge ID
     */
    public CaptchaChallenge generateChallenge(String sessionId) {
        // Clean up expired challenges
        cleanupExpiredChallenges();
        
        // Check if session has too many attempts
        CaptchaChallenge existing = challenges.get(sessionId);
        if (existing != null && existing.getAttempts() >= MAX_ATTEMPTS_PER_SESSION) {
            log.warn("Too many CAPTCHA attempts for session: {}", sessionId);
            return null;
        }
        
        // Generate mathematical challenge
        int a = RANDOM.nextInt(10) + 1; // 1-10
        int b = RANDOM.nextInt(10) + 1; // 1-10
        int operation = RANDOM.nextInt(3); // 0: +, 1: -, 2: *
        
        String question;
        int answer;
        
        switch (operation) {
            case 0: // Addition
                question = a + " + " + b + " = ?";
                answer = a + b;
                break;
            case 1: // Subtraction (ensure positive result)
                if (a < b) {
                    int temp = a;
                    a = b;
                    b = temp;
                }
                question = a + " - " + b + " = ?";
                answer = a - b;
                break;
            case 2: // Multiplication (keep numbers small)
                a = RANDOM.nextInt(5) + 1; // 1-5
                b = RANDOM.nextInt(5) + 1; // 1-5
                question = a + " × " + b + " = ?";
                answer = a * b;
                break;
            default:
                question = a + " + " + b + " = ?";
                answer = a + b;
        }
        
        // Create challenge
        String challengeId = generateChallengeId();
        CaptchaChallenge challenge = new CaptchaChallenge(
            challengeId, question, answer, LocalDateTime.now(), 0
        );
        
        // Store challenge
        challenges.put(sessionId, challenge);
        
        log.info("Generated CAPTCHA challenge for session: {} - Question: {}", sessionId, question);
        
        return challenge;
    }
    
    /**
     * Verify CAPTCHA response
     * @param sessionId The session identifier
     * @param challengeId The challenge ID
     * @param userAnswer The user's answer
     * @return true if answer is correct, false otherwise
     */
    public boolean verifyChallenge(String sessionId, String challengeId, String userAnswer) {
        CaptchaChallenge challenge = challenges.get(sessionId);
        
        if (challenge == null) {
            log.warn("No CAPTCHA challenge found for session: {}", sessionId);
            return false;
        }
        
        // Check if challenge ID matches
        if (!challengeId.equals(challenge.getChallengeId())) {
            log.warn("CAPTCHA challenge ID mismatch for session: {}", sessionId);
            return false;
        }
        
        // Check if challenge has expired
        if (challenge.isExpired()) {
            log.warn("CAPTCHA challenge expired for session: {}", sessionId);
            challenges.remove(sessionId);
            return false;
        }
        
        // Check if too many attempts
        if (challenge.getAttempts() >= MAX_ATTEMPTS_PER_SESSION) {
            log.warn("Too many CAPTCHA attempts for session: {}", sessionId);
            challenges.remove(sessionId);
            return false;
        }
        
        // Increment attempt count
        challenge.incrementAttempts();
        
        // Parse and verify answer
        try {
            int answer = Integer.parseInt(userAnswer.trim());
            boolean correct = answer == challenge.getCorrectAnswer();
            
            if (correct) {
                log.info("CAPTCHA challenge solved correctly for session: {}", sessionId);
                challenges.remove(sessionId); // Remove successful challenge
                return true;
            } else {
                log.warn("CAPTCHA challenge failed for session: {} - Expected: {}, Got: {}", 
                        sessionId, challenge.getCorrectAnswer(), answer);
                return false;
            }
            
        } catch (NumberFormatException e) {
            log.warn("Invalid CAPTCHA answer format for session: {} - Answer: {}", sessionId, userAnswer);
            return false;
        }
    }
    
    /**
     * Check if session has an active CAPTCHA challenge
     * @param sessionId The session identifier
     * @return true if challenge exists and is not expired
     */
    public boolean hasActiveChallenge(String sessionId) {
        CaptchaChallenge challenge = challenges.get(sessionId);
        return challenge != null && !challenge.isExpired();
    }
    
    /**
     * Remove challenge for session (e.g., after successful verification)
     * @param sessionId The session identifier
     */
    public void removeChallenge(String sessionId) {
        challenges.remove(sessionId);
        log.debug("Removed CAPTCHA challenge for session: {}", sessionId);
    }
    
    /**
     * Generate a unique challenge ID
     */
    private String generateChallengeId() {
        return "challenge_" + System.currentTimeMillis() + "_" + RANDOM.nextInt(1000);
    }
    
    /**
     * Clean up expired challenges
     */
    private void cleanupExpiredChallenges() {
        challenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * CAPTCHA challenge data structure
     */
    public static class CaptchaChallenge {
        private final String challengeId;
        private final String question;
        private final int correctAnswer;
        private final LocalDateTime createdAt;
        private int attempts;
        
        public CaptchaChallenge(String challengeId, String question, int correctAnswer, 
                              LocalDateTime createdAt, int attempts) {
            this.challengeId = challengeId;
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.createdAt = createdAt;
            this.attempts = attempts;
        }
        
        public boolean isExpired() {
            return createdAt.isBefore(LocalDateTime.now().minus(CHALLENGE_TIMEOUT_MINUTES, ChronoUnit.MINUTES));
        }
        
        public void incrementAttempts() {
            this.attempts++;
        }
        
        // Getters
        public String getChallengeId() { return challengeId; }
        public String getQuestion() { return question; }
        public int getCorrectAnswer() { return correctAnswer; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public int getAttempts() { return attempts; }
    }
}
