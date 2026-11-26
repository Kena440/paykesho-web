package org.paykesho.clientportal.transactions.pin;

import org.commons.entity.client.Client;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.transactions.controller.TransactionController;
import org.paykesho.clientportal.idempotency.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerPinTest {
    @Mock TransactionPinService pinService;
    @Mock IdempotencyService idempotencyService;
    @InjectMocks TransactionController controller;

    @Test
    void wrongPinStopsPaymentBeforeIdempotencyReservation() {
        Client client = new Client();
        client.setId(11);
        client.setEmail("client@example.test");
        ClientUserDetails principal = new ClientUserDetails(client);
        when(pinService.verify(11, "111111")).thenReturn(TransactionPinService.Result.WRONG);

        ResponseEntity<?> response = controller.saveSubmittedPayment(
                principal, Map.of("merchantCode", "MERCHANT1", "amount", "20", "pin", "111111"), "retry-key");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(pinService).verify(11, "111111");
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void missingPinIsRejectedBeforePaymentProcessing() {
        Client client = new Client();
        client.setId(11);
        client.setEmail("client@example.test");
        ClientUserDetails principal = new ClientUserDetails(client);
        when(pinService.verify(11, null)).thenReturn(TransactionPinService.Result.INVALID);

        ResponseEntity<?> response = controller.saveSubmittedPayment(
                principal, Map.of("merchantCode", "MERCHANT1", "amount", "20"), "retry-key");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(idempotencyService);
    }
}
