package org.paykesho.paymentservice.payments.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.dto.MoMoPaymentToPaykeshoRequest;
import org.commons.dto.MoMoPaymentToPaykeshoResponse;
import org.commons.entity.payments.Payment;
import org.commons.enums.PaymentMethod;
import org.commons.enums.PaymentStatus;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentDTO;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.card.CardPaymentDTO;
import org.paykesho.paymentservice.payments.dto.card.CardPaymentCallBackResponse;
import org.commons.dto.CardPaymentInitialResponse;
import org.commons.dto.CardPaymentRequest;
import org.paykesho.paymentservice.payments.dto.momo.*;
import org.paykesho.paymentservice.payments.repositories.PaymentRepository;
import org.paykesho.paymentservice.utils.PaymentConfigProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentService {

    private final PaymentConfigProperties paymentConfig;

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BankPaymentResponse runWalletToBankPayment(BankPaymentRequest bankPaymentRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            bankPaymentRequest.setReferenceNo(UUID.randomUUID().toString());
            BankPaymentDTO bankPaymentDTO = new BankPaymentDTO(paymentConfig, bankPaymentRequest);

            String requestBody = objectMapper.writeValueAsString(bankPaymentDTO);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BankPaymentResponse> responseEntity = restTemplate.exchange(paymentConfig.getUrl(), HttpMethod.POST, request, BankPaymentResponse.class);

            BankPaymentResponse bankPaymentResponse = responseEntity.getBody();
            handlePaymentResponse(bankPaymentResponse);
            return bankPaymentResponse;

        } catch (Exception e) {
            log.error("Error processing wallet-to-bank payment", e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    public MoMoPaymentToMerchantResponse runMoMoPaymentToMerchant(MoMoPaymentToMerchantRequest moMoPaymentToMerchantRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            moMoPaymentToMerchantRequest.setReferenceNo(UUID.randomUUID().toString());
            MoMoPaymentToMerchantDTO moMoPaymentToMerchantDTO = new MoMoPaymentToMerchantDTO(paymentConfig, moMoPaymentToMerchantRequest);
            BigDecimal amount = new BigDecimal(moMoPaymentToMerchantRequest.getAmount());

            String requestBody = objectMapper.writeValueAsString(moMoPaymentToMerchantDTO);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MoMoPaymentToMerchantResponse> responseEntity = restTemplate.exchange(paymentConfig.getUrl(), HttpMethod.POST, request, MoMoPaymentToMerchantResponse.class);

            MoMoPaymentToMerchantResponse moMoPaymentToMerchantResponse = responseEntity.getBody();
            handleMoMoPaymentToMerchantResponse(moMoPaymentToMerchantResponse, amount);
            return moMoPaymentToMerchantResponse;

        } catch (Exception e) {
            log.error("Error processing MoMo payment", e);
            throw new RuntimeException("MoMo payment processing failed", e);
        }
    }

    public MoMoPaymentToPaykeshoResponse runMoMoPaymentToPaykesho(MoMoPaymentToPaykeshoRequest moMoPaymentToPaykeshoRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            moMoPaymentToPaykeshoRequest.setReferenceNo(UUID.randomUUID().toString());
            MoMoPaymentToPaykeshoDTO moMoPaymentToPaykeshoDTO = new MoMoPaymentToPaykeshoDTO(paymentConfig, moMoPaymentToPaykeshoRequest);
            BigDecimal amount = new BigDecimal(moMoPaymentToPaykeshoRequest.getAmount());

            String requestBody = objectMapper.writeValueAsString(moMoPaymentToPaykeshoDTO);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MoMoPaymentToPaykeshoResponse> responseEntity = restTemplate.exchange(paymentConfig.getUrl(), HttpMethod.POST, request, MoMoPaymentToPaykeshoResponse.class);

            MoMoPaymentToPaykeshoResponse moMoPaymentToPaykeshoResponse = responseEntity.getBody();
            handleMoMoPaymentToPaykeshoResponse(moMoPaymentToPaykeshoResponse, amount);
            return moMoPaymentToPaykeshoResponse;

        } catch (Exception e) {
            log.error("Error processing MoMo payment", e);
            throw new RuntimeException("MoMo payment processing failed", e);
        }
    }

    public CardPaymentInitialResponse runCardPayment(CardPaymentRequest cardPaymentRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            cardPaymentRequest.setReferenceNo(UUID.randomUUID().toString());
            CardPaymentDTO cardPaymentDTO = new CardPaymentDTO(paymentConfig, cardPaymentRequest);
            BigDecimal amount = new BigDecimal(cardPaymentRequest.getAmount());

            String requestBody = objectMapper.writeValueAsString(cardPaymentDTO);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<CardPaymentInitialResponse> responseEntity = restTemplate.exchange(paymentConfig.getUrl(), HttpMethod.POST, request, CardPaymentInitialResponse.class);

            return responseEntity.getBody();

        } catch (Exception e) {
            log.error("Error processing card payment", e);
            throw new RuntimeException("card payment processing failed", e);
        }
    }

    private void handlePaymentResponse(BankPaymentResponse bankPaymentResponse) {
        if (bankPaymentResponse == null || bankPaymentResponse.getResponse_code() == null) {
            log.error("Invalid response from payment provider");
            throw new RuntimeException("Invalid response from payment provider");
        }

        switch (bankPaymentResponse.getResponse_code()) {
            case "100":
                saveSuccessfulBankPayment(bankPaymentResponse);
                break;
            case "990":
                logBankTransactionPending(bankPaymentResponse);
                break;
            case "995":
                saveFailedBankPayment(bankPaymentResponse, "Transaction failed");
                break;
            default:
                saveFailedBankPayment(bankPaymentResponse, "Unknown response code");
                break;
        }
    }

    private void handleMoMoPaymentToMerchantResponse(MoMoPaymentToMerchantResponse moMoPaymentToMerchantResponse, BigDecimal amount) {
        if (moMoPaymentToMerchantResponse == null || moMoPaymentToMerchantResponse.getResponse().getResponseCode() == null) {
            log.error("Invalid response from MoMo provider");
            throw new RuntimeException("Invalid response from MoMo provider");
        }

        switch (moMoPaymentToMerchantResponse.getResponse().getResponseCode()) {
            case "100":
                saveSuccessfulMoMoPaymentToMerchant(moMoPaymentToMerchantResponse, amount);
                break;
            case "990":
                logMoMoTransactionPendingToMerchant(moMoPaymentToMerchantResponse);
                break;
            case "995":
                saveFailedMoMoPaymentToMerchant(moMoPaymentToMerchantResponse, "Transaction failed", amount);
                break;
            default:
                saveFailedMoMoPaymentToMerchant(moMoPaymentToMerchantResponse, "Unknown response code", amount);
                break;
        }
    }

    private void handleMoMoPaymentToPaykeshoResponse(MoMoPaymentToPaykeshoResponse moMoPaymentToPaykeshoResponse, BigDecimal amount) {
        if (moMoPaymentToPaykeshoResponse == null || moMoPaymentToPaykeshoResponse.getResponse().getResponseCode() == null) {
            log.error("Invalid response from MoMo provider");
            throw new RuntimeException("Invalid response from MoMo provider");
        }

        switch (moMoPaymentToPaykeshoResponse.getResponse().getResponseCode()) {
            case "100":
                saveSuccessfulMoMoPaymentToPaykesho(moMoPaymentToPaykeshoResponse, amount);
                break;
            case "990":
                logMoMoTransactionPendingToPaykesho(moMoPaymentToPaykeshoResponse);
                break;
            case "995":
                saveFailedMoMoPaymentToPaykesho(moMoPaymentToPaykeshoResponse, "Transaction failed", amount);
                break;
            default:
                saveFailedMoMoPaymentToPaykesho(moMoPaymentToPaykeshoResponse, "Unknown response code", amount);
                break;
        }
    }

    private void saveSuccessfulBankPayment(BankPaymentResponse response) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getReference_no());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(new BigDecimal(response.getAmount()));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getOperatorreference());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.info("Bank payment successful: {}", response.getReference_no());
    }

    private void saveFailedBankPayment(BankPaymentResponse response, String reason) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getReference_no());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(new BigDecimal(response.getAmount()));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getOperatorreference());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.error("Bank payment failed [{}]: {}", response.getReference_no(), reason);
    }

    private void saveSuccessfulMoMoPaymentToMerchant(MoMoPaymentToMerchantResponse response, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getResponse().getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
        payment.setAmount(new BigDecimal(String.valueOf(amount)));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getResponse().getSenderId());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.info("MoMo payment successful: {}", response.getResponse().getReferenceNo());
    }

    private void saveSuccessfulMoMoPaymentToPaykesho(MoMoPaymentToPaykeshoResponse response, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getResponse().getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
        payment.setAmount(new BigDecimal(String.valueOf(amount)));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getResponse().getSenderId());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.info("MoMo payment successful: {}", response.getResponse().getReferenceNo());
    }

    private void saveFailedMoMoPaymentToMerchant(MoMoPaymentToMerchantResponse response, String reason, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getResponse().getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
        payment.setAmount(new BigDecimal(String.valueOf(amount)));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getResponse().getSenderId());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.error("MoMo payment failed [{}]: {}", response.getResponse().getReferenceNo(), reason);
    }

    private void saveFailedMoMoPaymentToPaykesho(MoMoPaymentToPaykeshoResponse response, String reason, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getResponse().getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
        payment.setAmount(new BigDecimal(String.valueOf(amount)));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getResponse().getSenderId());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.error("MoMo payment failed [{}]: {}", response.getResponse().getReferenceNo(), reason);
    }

    private void logBankTransactionPending(BankPaymentResponse response) {
        log.warn("Bank transaction pending: {}", response.getReference_no());
    }

    private void logMoMoTransactionPendingToMerchant(MoMoPaymentToMerchantResponse response) {
        log.warn("MoMo transaction pending: {}", response.getResponse().getReferenceNo());
    }

    private void logMoMoTransactionPendingToPaykesho(MoMoPaymentToPaykeshoResponse response) {
        log.warn("MoMo transaction pending: {}", response.getResponse().getReferenceNo());
    }

    public void handleZynlePayCallback(CardPaymentCallBackResponse callbackResponse) {
        if (callbackResponse == null || callbackResponse.getResponseCode() == null) {
            log.error("Invalid callback response from ZynlePay");
            throw new RuntimeException("Invalid callback response from ZynlePay");
        }

        switch (callbackResponse.getResponseCode()) {
            case "100":
                saveSuccessfulCardPayment(callbackResponse);
                break;
            case "990":
                logCardTransactionPending(callbackResponse);
                break;
            case "995":
                saveFailedCardPayment(callbackResponse, "Transaction failed");
                break;
            default:
                saveFailedCardPayment(callbackResponse, "Unknown response code");
                break;
        }
    }

    private void saveSuccessfulCardPayment(CardPaymentCallBackResponse response) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setAmount(new BigDecimal(response.getAmount()));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getOperatorReference());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.info("Card payment successful: {}", response.getReferenceNo());
    }

    private void saveFailedCardPayment(CardPaymentCallBackResponse response, String reason) {
        Payment payment = new Payment();
        payment.setPaymentReference(response.getReferenceNo());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setAmount(new BigDecimal(response.getAmount()));
        payment.setFinancialInstitution("Zynlepay");
        payment.setAccountNumber(response.getOperatorReference());
        payment.setPaymentDate(new Date());

        paymentRepository.save(payment);
        log.error("Card payment failed [{}]: {}", response.getReferenceNo(), reason);
    }

    private void logCardTransactionPending(CardPaymentCallBackResponse response) {
        log.warn("Card transaction pending: {}", response.getReferenceNo());
    }
}
