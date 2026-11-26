package org.paykesho.paymentservice.service;

import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.paykesho.paymentservice.repository.TransactionRepository;
import org.paykesho.paymentservice.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementReconciliationServiceTest {
    @Mock TransactionRepository transactions;
    @Mock VoucherRepository vouchers;

    private SettlementReconciliationService service() {
        return new SettlementReconciliationService(transactions, vouchers);
    }

    @Test
    void failedSettlementRestoresVoucherExactlyOnce() {
        Voucher voucher = new Voucher();
        voucher.setAvailableAmount(new BigDecimal("85.00"));
        Transaction transaction = new Transaction();
        transaction.setReference("PK-1");
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setVoucher(voucher);
        transaction.setSettlementStatus("PENDING");
        when(transactions.findByReferenceForUpdate("PK-1")).thenReturn(Optional.of(transaction));

        assertTrue(service().applyLencoResult("PK-1", "FAILED", "L-1", "declined"));
        assertEquals(new BigDecimal("100.00"), voucher.getAvailableAmount());
        assertEquals("FAILED", transaction.getSettlementStatus());

        assertTrue(service().applyLencoResult("PK-1", "FAILED", "L-1", "declined"));
        assertEquals(new BigDecimal("100.00"), voucher.getAvailableAmount());
        verify(vouchers, times(1)).save(voucher);
    }

    @Test
    void lateFailureCannotRefundSuccessfulPayment() {
        Voucher voucher = new Voucher();
        voucher.setAvailableAmount(new BigDecimal("85.00"));
        Transaction transaction = new Transaction();
        transaction.setReference("PK-2");
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setVoucher(voucher);
        transaction.setSettlementStatus("SUCCESSFUL");
        transaction.setSettled(true);
        when(transactions.findByReferenceForUpdate("PK-2")).thenReturn(Optional.of(transaction));

        assertTrue(service().applyLencoResult("PK-2", "FAILED", "L-2", "late failure"));
        assertEquals(new BigDecimal("85.00"), voucher.getAvailableAmount());
        verifyNoInteractions(vouchers);
        verify(transactions, never()).save(transaction);
    }

    @Test
    void unknownPaykeshoReferenceIsNotMarkedProcessed() {
        when(transactions.findByReferenceForUpdate("PK-missing")).thenReturn(Optional.empty());
        assertFalse(service().applyLencoResult("PK-missing", "SUCCESSFUL", "L-3", null));
        verifyNoInteractions(vouchers);
    }
}
