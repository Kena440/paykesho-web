package org.paykesho.clientportal.transactions.pin;

import org.commons.entity.client.ClientTransactionPin;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Server-side transaction PIN lifecycle and persistent three-attempt lockout. */
@Service
public class TransactionPinService {
    public enum Result { OK, NOT_SET, WRONG, LOCKED, ALREADY_SET, INVALID, SAME_PIN }

    private static final short MAX_ATTEMPTS = 3;
    private final ClientTransactionPinRepository repository;
    private final PasswordEncoder encoder;

    public TransactionPinService(ClientTransactionPinRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional
    public Result setup(Integer clientId, String pin) {
        if (clientId == null || !validPin(pin)) return Result.INVALID;
        if (repository.existsById(clientId)) return Result.ALREADY_SET;
        ClientTransactionPin record = new ClientTransactionPin();
        record.setClientId(clientId);
        record.setPinHash(encoder.encode(pin));
        record.setFailedAttempts((short) 0);
        record.setLastChangedAt(Instant.now());
        repository.save(record);
        return Result.OK;
    }

    @Transactional
    public Result verify(Integer clientId, String pin) {
        if (clientId == null || !validPin(pin)) return Result.INVALID;
        Optional<ClientTransactionPin> locked = repository.findByClientIdForUpdate(clientId);
        if (locked.isEmpty()) return Result.NOT_SET;
        ClientTransactionPin record = locked.get();
        if (record.getLockedAt() != null || record.getFailedAttempts() >= MAX_ATTEMPTS) return Result.LOCKED;
        if (!encoder.matches(pin, record.getPinHash())) {
            short attempts = (short) (record.getFailedAttempts() + 1);
            record.setFailedAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) record.setLockedAt(Instant.now());
            repository.save(record);
            return attempts >= MAX_ATTEMPTS ? Result.LOCKED : Result.WRONG;
        }
        record.setFailedAttempts((short) 0);
        record.setLockedAt(null);
        repository.save(record);
        return Result.OK;
    }

    @Transactional
    public Result change(Integer clientId, String currentPin, String newPin) {
        if (!validPin(newPin)) return Result.INVALID;
        if (currentPin != null && currentPin.equals(newPin)) return Result.SAME_PIN;
        Result verified = verify(clientId, currentPin);
        if (verified != Result.OK) return verified;
        ClientTransactionPin record = repository.findByClientIdForUpdate(clientId).orElse(null);
        if (record == null) return Result.NOT_SET;
        record.setPinHash(encoder.encode(newPin));
        record.setFailedAttempts((short) 0);
        record.setLockedAt(null);
        record.setLastChangedAt(Instant.now());
        repository.save(record);
        return Result.OK;
    }

    @Transactional(readOnly = true)
    public boolean isSet(Integer clientId) {
        return clientId != null && repository.existsById(clientId);
    }

    public static boolean validPin(String pin) {
        if (pin == null || !pin.matches("[0-9]{6}")) return false;
        if (pin.chars().allMatch(c -> c == pin.charAt(0))) return false;
        boolean ascending = true, descending = true;
        for (int i = 1; i < pin.length(); i++) {
            ascending &= pin.charAt(i) == pin.charAt(i - 1) + 1;
            descending &= pin.charAt(i) == pin.charAt(i - 1) - 1;
        }
        return !ascending && !descending;
    }
}
