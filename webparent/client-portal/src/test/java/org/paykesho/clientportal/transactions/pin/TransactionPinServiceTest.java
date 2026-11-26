package org.paykesho.clientportal.transactions.pin;

import org.commons.entity.client.ClientTransactionPin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionPinServiceTest {
    @Mock ClientTransactionPinRepository repository;

    private TransactionPinService service() {
        return new TransactionPinService(repository, new BCryptPasswordEncoder());
    }

    @Test
    void rejectsObviousOrMalformedPins() {
        assertFalse(TransactionPinService.validPin("123456"));
        assertFalse(TransactionPinService.validPin("654321"));
        assertFalse(TransactionPinService.validPin("000000"));
        assertFalse(TransactionPinService.validPin("12345"));
        assertFalse(TransactionPinService.validPin("12a456"));
        assertTrue(TransactionPinService.validPin("482951"));
    }

    @Test
    void setupStoresAdaptiveHashAndOnlyOnce() {
        when(repository.existsById(10)).thenReturn(false, true);
        TransactionPinService service = service();
        assertEquals(TransactionPinService.Result.OK, service.setup(10, "482951"));
        ArgumentCaptor<ClientTransactionPin> captor = ArgumentCaptor.forClass(ClientTransactionPin.class);
        verify(repository).save(captor.capture());
        ClientTransactionPin saved = captor.getValue();
        assertNotEquals("482951", saved.getPinHash());
        assertTrue(new BCryptPasswordEncoder().matches("482951", saved.getPinHash()));
        assertNotNull(saved.getLastChangedAt());
        assertEquals(TransactionPinService.Result.ALREADY_SET, service.setup(10, "482951"));
    }

    @Test
    void thirdWrongAttemptLocksAndCorrectPinResetsCounter() {
        ClientTransactionPin row = new ClientTransactionPin();
        row.setClientId(10);
        row.setPinHash(new BCryptPasswordEncoder().encode("482951"));
        row.setFailedAttempts((short) 0);
        when(repository.findByClientIdForUpdate(10)).thenReturn(Optional.of(row));
        TransactionPinService service = service();

        assertEquals(TransactionPinService.Result.WRONG, service.verify(10, "111111"));
        assertEquals(1, row.getFailedAttempts());
        assertEquals(TransactionPinService.Result.WRONG, service.verify(10, "111111"));
        assertEquals(2, row.getFailedAttempts());
        assertEquals(TransactionPinService.Result.LOCKED, service.verify(10, "111111"));
        assertEquals(3, row.getFailedAttempts());
        assertNotNull(row.getLockedAt());
        assertEquals(TransactionPinService.Result.LOCKED, service.verify(10, "482951"));

        // Recovery must clear the lock; successful verification resets the count.
        row.setLockedAt(null);
        row.setFailedAttempts((short) 0);
        assertEquals(TransactionPinService.Result.OK, service.verify(10, "482951"));
        assertEquals(0, row.getFailedAttempts());
        assertNull(row.getLockedAt());
    }
    @Test
    void changePinRequiresOldPinAndReplacesHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        ClientTransactionPin row = new ClientTransactionPin();
        row.setClientId(10);
        row.setPinHash(encoder.encode("482951"));
        row.setFailedAttempts((short) 0);
        when(repository.findByClientIdForUpdate(10)).thenReturn(Optional.of(row));
        TransactionPinService service = new TransactionPinService(repository, encoder);

        assertEquals(TransactionPinService.Result.SAME_PIN, service.change(10, "482951", "482951"));
        assertEquals(TransactionPinService.Result.OK, service.change(10, "482951", "735194"));
        assertTrue(encoder.matches("735194", row.getPinHash()));
        assertFalse(encoder.matches("482951", row.getPinHash()));
        assertEquals(TransactionPinService.Result.WRONG, service.verify(10, "482951"));
        assertEquals(TransactionPinService.Result.OK, service.verify(10, "735194"));
    }

}
