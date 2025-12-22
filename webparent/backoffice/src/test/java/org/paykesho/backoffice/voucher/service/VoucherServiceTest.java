package org.paykesho.backoffice.voucher.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.enums.VoucherStatus;
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
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.voucher.repository.VouchersRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService (Backoffice) Unit Tests")
class VoucherServiceTest extends BaseUnitTest {

    @Mock
    private VouchersRepository vouchersRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private VoucherService voucherService;

    private Voucher testVoucher;
    private String testUuid;

    @BeforeEach
    void setUp() {
        testUuid = UUID.randomUUID().toString();
        testVoucher = TestDataBuilders.voucher()
                .withId(1)
                .withReference("VCR001")
                .withStatus(VoucherStatus.PENDING)
                .build();
        testVoucher.setUuid(testUuid);
    }

    @Test
    @DisplayName("Should get voucher by UUID successfully")
    void testGetVoucherByUuid_Success() throws PaykeshoError {
        // Given
        when(vouchersRepository.findByUuid(testUuid)).thenReturn(Optional.of(testVoucher));

        // When
        Voucher result = voucherService.getVoucherByUuid(testUuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(testUuid);
        assertThat(result.getReference()).isEqualTo("VCR001");
        verify(vouchersRepository, times(1)).findByUuid(testUuid);
    }

    @Test
    @DisplayName("Should throw exception when UUID is null")
    void testGetVoucherByUuid_NullUuid() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getVoucherByUuid(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("Invalid UUID parameter");

        verify(vouchersRepository, never()).findByUuid(anyString());
    }

    @Test
    @DisplayName("Should throw exception when UUID is empty")
    void testGetVoucherByUuid_EmptyUuid() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getVoucherByUuid("   "))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("Invalid UUID parameter");
    }

    @Test
    @DisplayName("Should throw exception when UUID format is invalid")
    void testGetVoucherByUuid_InvalidFormat() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getVoucherByUuid("invalid-uuid"))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("Invalid UUID format");
    }

    @Test
    @DisplayName("Should throw exception when voucher not found")
    void testGetVoucherByUuid_NotFound() {
        // Given
        when(vouchersRepository.findByUuid(testUuid)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> voucherService.getVoucherByUuid(testUuid))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("Could not find voucher");
    }

    @Test
    @DisplayName("Should approve voucher by UUID")
    void testApproveVoucherByUuid_Success() throws PaykeshoError {
        // Given
        when(vouchersRepository.findByUuid(testUuid)).thenReturn(Optional.of(testVoucher));
        doNothing().when(vouchersRepository).updateApprovedStatus(1, true);

        // When
        voucherService.approveVoucherByUuid(testUuid);

        // Then
        verify(vouchersRepository, times(1)).findByUuid(testUuid);
        verify(vouchersRepository, times(1)).updateApprovedStatus(1, true);
    }

    @Test
    @DisplayName("Should save voucher successfully")
    void testSave_Success() throws PaykeshoError {
        // Given
        when(vouchersRepository.save(testVoucher)).thenReturn(testVoucher);

        // When
        Voucher result = voucherService.save(testVoucher);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testVoucher);
        verify(vouchersRepository, times(1)).save(testVoucher);
    }

    @Test
    @DisplayName("Should throw exception when voucher is null")
    void testSave_NullVoucher() {
        // When/Then
        assertThatThrownBy(() -> voucherService.save(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("Null parameter id");

        verify(vouchersRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get approved and valid client vouchers")
    void testGetApprovedAndValidClientVouchers_Success() throws PaykeshoError {
        // Given
        Client testClient = TestDataBuilders.client().withId(1).build();
        List<Voucher> vouchers = Arrays.asList(testVoucher);
        when(vouchersRepository.getApprovedAndValidVouchers(testClient)).thenReturn(vouchers);

        // When
        List<Voucher> result = voucherService.getApprovedAndValidClientVouchers(testClient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(vouchersRepository, times(1)).getApprovedAndValidVouchers(testClient);
    }

    @Test
    @DisplayName("Should throw exception when client is null")
    void testGetApprovedAndValidClientVouchers_NullClient() {
        // When/Then
        assertThatThrownBy(() -> voucherService.getApprovedAndValidClientVouchers(null))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("no client id");
    }
}
