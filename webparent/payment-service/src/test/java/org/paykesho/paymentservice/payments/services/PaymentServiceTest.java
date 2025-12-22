package org.paykesho.paymentservice.payments.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commons.dto.CardPaymentInitialResponse;
import org.commons.dto.CardPaymentRequest;
import org.commons.dto.MoMoPaymentToPaykeshoRequest;
import org.commons.dto.MoMoPaymentToPaykeshoResponse;
import org.commons.entity.IdempotencyRecord;
import org.commons.entity.payments.Payment;
import org.commons.enums.PaymentMethod;
import org.commons.enums.PaymentStatus;
import org.commons.test.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.paymentservice.idempotency.service.IdempotencyService;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.card.CardPaymentCallBackResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantResponse;
import org.paykesho.paymentservice.payments.repositories.PaymentRepository;
import org.paykesho.paymentservice.utils.PaymentConfigProperties;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest extends BaseUnitTest {

    @Mock
    private PaymentConfigProperties paymentConfig;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private String testPaymentUrl;
    private String testReferenceNo;

    @BeforeEach
    void setUp() {
        testPaymentUrl = "https://api.paymentgateway.com/pay";
        testReferenceNo = UUID.randomUUID().toString();
        lenient().when(paymentConfig.getUrl()).thenReturn(testPaymentUrl);
        
        // Setup WebClient mock chain (lenient since not all tests use it)
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        lenient().doReturn(requestBodySpec).when(requestBodySpec).bodyValue(anyString());
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Should process bank payment successfully")
    void testRunWalletToBankPayment_Success() throws JsonProcessingException {
        // Given
        BankPaymentRequest request = new BankPaymentRequest();
        request.setAmount("1000.00");
        request.setReceiverId("RECEIVER123");

        BankPaymentResponse mockResponse = new BankPaymentResponse();
        mockResponse.setResponse_code("100");
        mockResponse.setReference_no(testReferenceNo);
        mockResponse.setAmount("1000.00");
        mockResponse.setOperatorreference("OP123");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(responseSpec.bodyToMono(BankPaymentResponse.class)).thenReturn(Mono.just(mockResponse));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BankPaymentResponse result = paymentService.runWalletToBankPayment(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getResponse_code()).isEqualTo("100");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }

    @Test
    @DisplayName("Should handle bank payment failure")
    void testRunWalletToBankPayment_Failure() throws JsonProcessingException {
        // Given
        BankPaymentRequest request = new BankPaymentRequest();
        request.setAmount("1000.00");
        request.setReceiverId("RECEIVER123");

        BankPaymentResponse mockResponse = new BankPaymentResponse();
        mockResponse.setResponse_code("995");
        mockResponse.setReference_no(testReferenceNo);
        mockResponse.setAmount("1000.00");
        mockResponse.setOperatorreference("OP123");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(responseSpec.bodyToMono(BankPaymentResponse.class)).thenReturn(Mono.just(mockResponse));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BankPaymentResponse result = paymentService.runWalletToBankPayment(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getResponse_code()).isEqualTo("995");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Should handle JSON serialization error")
    void testRunWalletToBankPayment_JsonError() throws JsonProcessingException {
        // Given
        BankPaymentRequest request = new BankPaymentRequest();
        request.setAmount("1000.00");
        request.setReceiverId("RECEIVER123");

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("JSON error") {});

        // When/Then
        assertThatThrownBy(() -> paymentService.runWalletToBankPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment request formatting failed");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle network error")
    void testRunWalletToBankPayment_NetworkError() throws JsonProcessingException {
        // Given
        BankPaymentRequest request = new BankPaymentRequest();
        request.setAmount("1000.00");
        request.setReceiverId("RECEIVER123");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(responseSpec.bodyToMono(BankPaymentResponse.class))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        // When/Then
        assertThatThrownBy(() -> paymentService.runWalletToBankPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment gateway communication failed");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process MoMo payment to merchant successfully")
    void testRunMoMoPaymentToMerchant_Success() throws JsonProcessingException {
        // Given
        MoMoPaymentToMerchantRequest request = new MoMoPaymentToMerchantRequest();
        request.setAmount("500.00");
        request.setReceiverId("RECEIVER123");

        MoMoPaymentToMerchantResponse.Response responseData = new MoMoPaymentToMerchantResponse.Response();
        responseData.setResponseCode("100");
        responseData.setReferenceNo(testReferenceNo);
        responseData.setSenderId("SENDER123");

        MoMoPaymentToMerchantResponse mockResponse = new MoMoPaymentToMerchantResponse();
        mockResponse.setResponse(responseData);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(responseSpec.bodyToMono(MoMoPaymentToMerchantResponse.class)).thenReturn(Mono.just(mockResponse));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MoMoPaymentToMerchantResponse result = paymentService.runMoMoPaymentToMerchant(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getResponse().getResponseCode()).isEqualTo("100");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should process card payment successfully")
    void testRunCardPayment_Success() throws JsonProcessingException {
        // Given
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount("2000.00");

        CardPaymentInitialResponse.Response responseData = new CardPaymentInitialResponse.Response();
        responseData.setResponseCode("100");
        responseData.setReferenceNo(testReferenceNo);
        
        CardPaymentInitialResponse mockResponse = new CardPaymentInitialResponse();
        mockResponse.setResponse(responseData);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(responseSpec.bodyToMono(CardPaymentInitialResponse.class)).thenReturn(Mono.just(mockResponse));

        // When
        CardPaymentInitialResponse result = paymentService.runCardPayment(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getResponseCode()).isEqualTo("100");
    }

    @Test
    @DisplayName("Should handle webhook callback with idempotency check")
    void testHandleZynlePayCallback_WithIdempotency() throws JsonProcessingException {
        // Given
        CardPaymentCallBackResponse callbackResponse = new CardPaymentCallBackResponse();
        callbackResponse.setResponseCode("100");
        callbackResponse.setReferenceNo(testReferenceNo);
        callbackResponse.setAmount("1000.00");
        callbackResponse.setOperatorReference("OP123");

        String idempotencyKey = "WEBHOOK_ZYNLEPAY_" + testReferenceNo;
        IdempotencyRecord existingRecord = new IdempotencyRecord();
        existingRecord.setStatus("COMPLETED");

        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(Optional.of(existingRecord));

        // When
        paymentService.handleZynlePayCallback(callbackResponse);

        // Then
        verify(idempotencyService, times(1)).checkIdempotency(idempotencyKey);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process webhook callback successfully")
    void testHandleZynlePayCallback_Success() throws JsonProcessingException {
        // Given
        CardPaymentCallBackResponse callbackResponse = new CardPaymentCallBackResponse();
        callbackResponse.setResponseCode("100");
        callbackResponse.setReferenceNo(testReferenceNo);
        callbackResponse.setAmount("1000.00");
        callbackResponse.setOperatorReference("OP123");

        String idempotencyKey = "WEBHOOK_ZYNLEPAY_" + testReferenceNo;

        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(Optional.empty());
        when(idempotencyService.createIdempotencyRecord(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new IdempotencyRecord());
        when(objectMapper.writeValueAsString(callbackResponse)).thenReturn("{}");
        when(paymentRepository.findByPaymentReference(testReferenceNo)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleZynlePayCallback(callbackResponse);

        // Then
        verify(idempotencyService, times(1)).checkIdempotency(idempotencyKey);
        verify(idempotencyService, times(1)).createIdempotencyRecord(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(idempotencyService, times(1)).markAsCompleted(eq(idempotencyKey), anyInt(), anyString(), isNull());
    }

    @Test
    @DisplayName("Should check if payment already processed")
    void testIsPaymentAlreadyProcessed_AlreadyProcessed() {
        // Given
        String idempotencyKey = "PAYMENT_KEY_123";
        IdempotencyRecord record = new IdempotencyRecord();
        record.setStatus("COMPLETED");

        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(Optional.of(record));

        // When
        boolean result = paymentService.isPaymentAlreadyProcessed(idempotencyKey);

        // Then
        assertThat(result).isTrue();
        verify(idempotencyService, times(1)).checkIdempotency(idempotencyKey);
    }

    @Test
    @DisplayName("Should return false for new payment")
    void testIsPaymentAlreadyProcessed_NotProcessed() {
        // Given
        String idempotencyKey = "PAYMENT_KEY_123";
        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(Optional.empty());

        // When
        boolean result = paymentService.isPaymentAlreadyProcessed(idempotencyKey);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should mark payment as processed")
    void testMarkPaymentAsProcessed() {
        // Given
        String idempotencyKey = "PAYMENT_KEY_123";
        String paymentReference = "PAY_REF_456";

        // When
        paymentService.markPaymentAsProcessed(idempotencyKey, paymentReference);

        // Then
        verify(idempotencyService, times(1)).markAsCompleted(eq(idempotencyKey), eq(200), contains(paymentReference), isNull());
    }
}
