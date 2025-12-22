package org.paykesho.organizationportal.repayments.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.commons.enums.ProcessingStatus;
import org.commons.test.BaseUnitTest;
import org.commons.test.TestDataBuilders;
import org.commons.utils.PaykeshoError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.organizationportal.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.organizationportal.repayments.repository.PayrollDeductionRepository;
import org.paykesho.organizationportal.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.organizationportal.utils.PaykeshoReferenceService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepaymentService Unit Tests")
class RepaymentServiceTest extends BaseUnitTest {

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;

    @Mock
    private PayrollDeductionRepository payrollDeductionRepository;

    @Mock
    private PaykeshoReferenceService paykeshoReferenceService;

    @Mock
    private VouchersService vouchersService;

    @InjectMocks
    private RepaymentService repaymentService;

    private Client testClient;
    private Voucher testVoucher1;
    private Voucher testVoucher2;

    @BeforeEach
    void setUp() {
        testClient = TestDataBuilders.client()
                .withId(1)
                .withEmail("test@example.com")
                .build();

        testVoucher1 = TestDataBuilders.voucher()
                .withId(1)
                .withClient(testClient)
                .withAmount(new BigDecimal("1000.00"))
                .build();

        testVoucher2 = TestDataBuilders.voucher()
                .withId(2)
                .withClient(testClient)
                .withAmount(new BigDecimal("500.00"))
                .build();
    }

    @Test
    @DisplayName("Should create repayment schedule successfully")
    void testCreateRepaymentSchedule_Success() throws PaykeshoError {
        // Given
        RepaymentScheduleRequest request = new RepaymentScheduleRequest();
        request.setClient(testClient);

        String paykeshoRef = "ITEM_REQ_RPS_001";
        when(paykeshoReferenceService.generateCode("ITEM_REQ_RPS")).thenReturn(paykeshoRef);
        when(vouchersService.getApprovedAndValidClientVouchers(testClient))
                .thenReturn(Arrays.asList(testVoucher1, testVoucher2));
        when(repaymentScheduleRepository.save(any(RepaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        repaymentService.createRepaymentSchedule(request);

        // Then
        ArgumentCaptor<RepaymentSchedule> captor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(repaymentScheduleRepository, times(1)).save(captor.capture());
        RepaymentSchedule savedSchedule = captor.getValue();

        assertThat(savedSchedule.getClient()).isEqualTo(testClient);
        assertThat(savedSchedule.getReference()).isEqualTo(paykeshoRef);
        assertThat(savedSchedule.getAmountDue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(savedSchedule.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedSchedule.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedSchedule.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when reference generation fails")
    void testCreateRepaymentSchedule_ReferenceGenerationFailure() throws PaykeshoError {
        // Given
        RepaymentScheduleRequest request = new RepaymentScheduleRequest();
        request.setClient(testClient);

        when(paykeshoReferenceService.generateCode("ITEM_REQ_RPS")).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> repaymentService.createRepaymentSchedule(request))
                .isInstanceOf(PaykeshoError.class)
                .hasMessageContaining("FAILED_GEN_PAYKESHO_REF");

        verify(repaymentScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process payroll deductions successfully")
    void testProcessPayrollDeductions_Success() throws PaykeshoError {
        // Given
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(1);
        schedule.setClient(testClient);
        schedule.setAmountDue(new BigDecimal("1000.00"));
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setStatus(PaymentStatus.PENDING);

        List<RepaymentSchedule> dueSchedules = Arrays.asList(schedule);

        String paykeshoRef = "ITEM_REQ_PRD_001";
        when(paykeshoReferenceService.generateCode("ITEM_REQ_PRD")).thenReturn(paykeshoRef);
        when(repaymentScheduleRepository.save(any(RepaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(payrollDeductionRepository.save(any(PayrollDeduction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        repaymentService.processPayrollDeductions(dueSchedules);

        // Then
        ArgumentCaptor<RepaymentSchedule> scheduleCaptor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(repaymentScheduleRepository, times(1)).save(scheduleCaptor.capture());
        RepaymentSchedule savedSchedule = scheduleCaptor.getValue();

        assertThat(savedSchedule.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedSchedule.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        ArgumentCaptor<PayrollDeduction> deductionCaptor = ArgumentCaptor.forClass(PayrollDeduction.class);
        verify(payrollDeductionRepository, times(1)).save(deductionCaptor.capture());
        PayrollDeduction savedDeduction = deductionCaptor.getValue();

        assertThat(savedDeduction.getClient()).isEqualTo(testClient);
        assertThat(savedDeduction.getReference()).isEqualTo(paykeshoRef);
        assertThat(savedDeduction.getAmountDeducted()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedDeduction.getStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should handle partial payment correctly")
    void testProcessPayrollDeductions_PartialPayment() throws PaykeshoError {
        // Given
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(1);
        schedule.setClient(testClient);
        schedule.setAmountDue(new BigDecimal("1000.00"));
        schedule.setAmountPaid(new BigDecimal("300.00"));
        schedule.setStatus(PaymentStatus.PENDING);

        List<RepaymentSchedule> dueSchedules = Arrays.asList(schedule);

        String paykeshoRef = "ITEM_REQ_PRD_001";
        when(paykeshoReferenceService.generateCode("ITEM_REQ_PRD")).thenReturn(paykeshoRef);
        when(repaymentScheduleRepository.save(any(RepaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(payrollDeductionRepository.save(any(PayrollDeduction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        repaymentService.processPayrollDeductions(dueSchedules);

        // Then
        ArgumentCaptor<RepaymentSchedule> scheduleCaptor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(repaymentScheduleRepository, times(1)).save(scheduleCaptor.capture());
        RepaymentSchedule savedSchedule = scheduleCaptor.getValue();

        assertThat(savedSchedule.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedSchedule.getStatus()).isEqualTo(PaymentStatus.COMPLETED); // Completed since amountPaid >= amountDue
    }

    @Test
    @DisplayName("Should skip non-pending schedules")
    void testProcessPayrollDeductions_NonPendingSchedule() throws PaykeshoError {
        // Given
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(1);
        schedule.setClient(testClient);
        schedule.setAmountDue(new BigDecimal("1000.00"));
        schedule.setStatus(PaymentStatus.COMPLETED); // Already completed

        List<RepaymentSchedule> dueSchedules = Arrays.asList(schedule);

        // When
        repaymentService.processPayrollDeductions(dueSchedules);

        // Then
        verify(repaymentScheduleRepository, never()).save(any());
        verify(payrollDeductionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get repayment schedule by client ID")
    void testGetRepaymentSchedule() {
        // Given
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(1);
        schedule.setClient(testClient);

        when(repaymentScheduleRepository.findByClientId(1L)).thenReturn(Arrays.asList(schedule));

        // When
        List<RepaymentSchedule> result = repaymentService.getRepaymentSchedule(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(repaymentScheduleRepository, times(1)).findByClientId(1L);
    }

    @Test
    @DisplayName("Should convert LocalDate to Date")
    void testConvertToDate() {
        // Given
        LocalDate localDate = LocalDate.of(2024, 1, 15);

        // When
        Date result = repaymentService.convertToDate(localDate);

        // Then
        assertThat(result).isNotNull();
    }
}
