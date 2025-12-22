package org.paykesho.clientportal.voucher.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.test.BaseUnitTest;
import org.commons.test.TestDataBuilders;
import org.commons.utils.PaykeshoError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService (Client Portal) Unit Tests")
class VoucherServiceTest extends BaseUnitTest {

    @Mock
    private VouchersRepository vouchersRepository;

    @InjectMocks
    private VoucherService voucherService;

    private Client testClient;
    private Voucher testVoucher1;
    private Voucher testVoucher2;

    @BeforeEach
    void setUp() {
        testClient = TestDataBuilders.client()
                .withEmail("test@example.com")
                .withId(1)
                .build();

        testVoucher1 = TestDataBuilders.voucher()
                .withId(1)
                .withReference("VCR001")
                .withClient(testClient)
                .withAmount(new BigDecimal("1000.00"))
                .build();

        testVoucher2 = TestDataBuilders.voucher()
                .withId(2)
                .withReference("VCR002")
                .withClient(testClient)
                .withAmount(new BigDecimal("500.00"))
                .build();
    }

    @Test
    @DisplayName("Should get client vouchers successfully")
    void testGetClientVouchers_Success() throws PaykeshoError {
        // Given
        List<Voucher> vouchers = Arrays.asList(testVoucher1, testVoucher2);
        when(vouchersRepository.getVouchersByClient(testClient)).thenReturn(vouchers);

        // When
        List<Voucher> result = voucherService.getClientVouchers(testClient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testVoucher1, testVoucher2);
        verify(vouchersRepository, times(1)).getVouchersByClient(testClient);
    }

    @Test
    @DisplayName("Should throw exception when client is null")
    void testGetClientVouchers_NullClient() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getClientVouchers(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("no client id");

        verify(vouchersRepository, never()).getVouchersByClient(any());
    }

    @Test
    @DisplayName("Should save voucher successfully")
    void testSave_Success() throws PaykeshoError {
        // Given
        when(vouchersRepository.save(testVoucher1)).thenReturn(testVoucher1);

        // When
        Voucher result = voucherService.save(testVoucher1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testVoucher1);
        verify(vouchersRepository, times(1)).save(testVoucher1);
    }

    @Test
    @DisplayName("Should throw exception when voucher is null")
    void testSave_NullVoucher() {
        // When/Then
        assertThatThrownBy(() -> voucherService.save(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("no voucher");

        verify(vouchersRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get approved and valid client vouchers")
    void testGetApprovedAndValidClientVouchers_Success() throws PaykeshoError {
        // Given
        List<Voucher> approvedVouchers = Arrays.asList(testVoucher1);
        when(vouchersRepository.getApprovedAndValidVouchers(testClient)).thenReturn(approvedVouchers);

        // When
        List<Voucher> result = voucherService.getApprovedAndValidClientVouchers(testClient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(testVoucher1);
        verify(vouchersRepository, times(1)).getApprovedAndValidVouchers(testClient);
    }

    @Test
    @DisplayName("Should throw exception when client is null for approved vouchers")
    void testGetApprovedAndValidClientVouchers_NullClient() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getApprovedAndValidClientVouchers(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("no client id");
    }

    @Test
    @DisplayName("Should calculate approved and valid vouchers balance")
    void testGetApprovedAndValidClientVouchersBalance_Success() throws PaykeshoError {
        // Given
        List<Voucher> approvedVouchers = Arrays.asList(testVoucher1, testVoucher2);
        when(vouchersRepository.getApprovedAndValidVouchers(testClient)).thenReturn(approvedVouchers);

        // When
        BigDecimal result = voucherService.getApprovedAndValidClientVouchersBalance(testClient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(vouchersRepository, times(1)).getApprovedAndValidVouchers(testClient);
    }

    @Test
    @DisplayName("Should return zero balance when no approved vouchers")
    void testGetApprovedAndValidClientVouchersBalance_NoVouchers() throws PaykeshoError {
        // Given
        when(vouchersRepository.getApprovedAndValidVouchers(testClient)).thenReturn(Collections.emptyList());

        // When
        BigDecimal result = voucherService.getApprovedAndValidClientVouchersBalance(testClient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when client is null for balance calculation")
    void testGetApprovedAndValidClientVouchersBalance_NullClient() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getApprovedAndValidClientVouchersBalance(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("no client id");
    }
}
