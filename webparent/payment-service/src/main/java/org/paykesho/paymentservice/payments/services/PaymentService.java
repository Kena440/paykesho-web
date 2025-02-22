package org.paykesho.paymentservice.payments.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.payments.Payment;
import org.commons.enums.PaymentMethod;
import org.commons.enums.PaymentStatus;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentDTO;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentDTO;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentResponse;
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

    public MoMoPaymentResponse runMoMoPayment(MoMoPaymentRequest moMoPaymentRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            moMoPaymentRequest.setReferenceNo(UUID.randomUUID().toString());
            MoMoPaymentDTO moMoPaymentDTO = new MoMoPaymentDTO(paymentConfig, moMoPaymentRequest);
            BigDecimal amount = new BigDecimal(moMoPaymentRequest.getAmount());

            String requestBody = objectMapper.writeValueAsString(moMoPaymentDTO);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MoMoPaymentResponse> responseEntity = restTemplate.exchange(paymentConfig.getUrl(), HttpMethod.POST, request, MoMoPaymentResponse.class);

            MoMoPaymentResponse moMoPaymentResponse = responseEntity.getBody();
            handleMoMoPaymentResponse(moMoPaymentResponse, amount);
            return moMoPaymentResponse;

        } catch (Exception e) {
            log.error("Error processing MoMo payment", e);
            throw new RuntimeException("MoMo payment processing failed", e);
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

    private void handleMoMoPaymentResponse(MoMoPaymentResponse moMoPaymentResponse, BigDecimal amount) {
        if (moMoPaymentResponse == null || moMoPaymentResponse.getResponse().getResponseCode() == null) {
            log.error("Invalid response from MoMo provider");
            throw new RuntimeException("Invalid response from MoMo provider");
        }

        switch (moMoPaymentResponse.getResponse().getResponseCode()) {
            case "100":
                saveSuccessfulMoMoPayment(moMoPaymentResponse, amount);
                break;
            case "990":
                logMoMoTransactionPending(moMoPaymentResponse);
                break;
            case "995":
                saveFailedMoMoPayment(moMoPaymentResponse, "Transaction failed", amount);
                break;
            default:
                saveFailedMoMoPayment(moMoPaymentResponse, "Unknown response code", amount);
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

    private void saveSuccessfulMoMoPayment(MoMoPaymentResponse response, BigDecimal amount) {
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

    private void saveFailedMoMoPayment(MoMoPaymentResponse response, String reason, BigDecimal amount) {
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

    private void logMoMoTransactionPending(MoMoPaymentResponse response) {
        log.warn("MoMo transaction pending: {}", response.getResponse().getReferenceNo());
    }
}
