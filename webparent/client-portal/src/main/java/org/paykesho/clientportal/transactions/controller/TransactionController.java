package org.paykesho.clientportal.transactions.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.MerchantDto;
import org.paykesho.clientportal.api.dto.PaymentDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.notifications.service.NotificationService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.transactions.service.TransactionsService;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private NotificationService notificationService;

    @GetMapping("")
    public String showClientTransactions(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Transaction> transactionList = transactionsService.getClientTransactions(client);
        model.addAttribute("customer", client);
        model.addAttribute("transactionList", transactionList);
        model.addAttribute("pageTitle", "My Transactions");

        return "transactions/transactions";
    }

    @GetMapping("/initiate")
    public String showClientTransactionPaymentForm(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Voucher> voucherList = voucherService.getApprovedAndValidClientVouchers(client);
        model.addAttribute("voucherList", voucherList);
        model.addAttribute("paymentDto", new PaymentDto());
        model.addAttribute("pageTitle", "Initiate Transaction");

        return "transactions/initiate";
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> saveSubmittedPayment(@AuthenticationPrincipal ClientUserDetails loggedInUser, @RequestBody Map<String, String> requestBody) {

        String merchantCode = requestBody.get("merchantCode");
        BigDecimal amount = BigDecimal.valueOf(Long.parseLong(requestBody.get("amount")));

        try {
            Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

            MerchantDto merchant = merchantService.getMerchantByCode(merchantCode);
            if (merchant == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid merchant code."));
            }

            BigDecimal availableBalance = clientService.getClientBalance(client).getAvailableBalance();

            if (availableBalance.compareTo(amount) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Insufficient balance."));
            }

            Transaction trx = runPaymentSubmission(client, merchant, amount);

            if (trx != null && trx.getReference() != null && !trx.getReference().isEmpty()) {
                return updateClientBalance(amount, client, availableBalance);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Payment failed. Please try again."));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid amount format."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "An error occurred while processing your payment."));
        }
    }

    private ResponseEntity<Map<String, Boolean>> updateClientBalance(BigDecimal amount, Client client, BigDecimal currentBalance) {
        ClientBalance clientBalance = clientService.getClientBalance(client);
        clientBalance.setCurrentBalance(currentBalance.subtract(amount));
        clientBalance.setAvailableBalance(currentBalance.subtract(amount));
        clientService.saveClientBalance(clientBalance);
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    private Transaction runPaymentSubmission(Client client, MerchantDto merchantDto, BigDecimal amount) throws PaykeshoError {

        Transaction savedTxn = null;
        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_TRX");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        BigDecimal availableBalance = voucherService.getApprovedAndValidClientVouchersBalance(client);
        if (availableBalance == null || availableBalance.compareTo(amount) < 0) {
            throw new PaykeshoError("error processing payment. payment amount is larger or insufficient balance.");
        }

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setMerchant(merchantService.getMerchantByPaykeshoRef(merchantDto.getCode()));
        transaction.setAmount(amount);
        transaction.setReference(paykeshoRef);
        transaction.setNaration(paykeshoRef);
        transaction.setTxnDate(new Date());
        transaction.setSettled(false);

        savedTxn = transactionsService.save(transaction);

        sendNotification(savedTxn);
        return savedTxn;
    }

    private void sendNotification(Transaction transaction) {
        // Generate messages
        String emailMessage = generateEmailMessage(transaction, "PAYMENT");
        String smsMessage = generateSmsMessage(transaction, "PAYMENT");

        // Send email notification
        boolean emailSent = notificationService.sendNotification(transaction.getClient().getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            log.info("Email sent successfully to " + transaction.getClient().getEmail());
        } else {
            log.error("Failed to send email to " + transaction.getClient().getEmail());
        }

        // Send SMS notification
        boolean smsSent = notificationService.sendNotification(transaction.getClient().getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());
        if (smsSent) {
            log.info("SMS sent successfully to " + transaction.getClient().getPhoneNumber());
        } else {
            log.error("Failed to send SMS to " + transaction.getClient().getPhoneNumber());
        }
    }

    private String generateEmailMessage(Transaction transaction, String actionType) {
        StringBuilder message = new StringBuilder("<p>Dear ").append(transaction.getClient().getFullName()).append(",</p>");

        if ("PAYMENT".equals(actionType)) {
            message.append("<p>Your payment request has been processed successfully. ").append("Transaction Amount: K").append(transaction.getAmount()).append("Merchant: ").append(transaction.getMerchant().getName()).append("Transaction Reference: ").append(transaction.getReference()).append(".</p>");
        } else {
            message.append("<p>Unknown action type.</p>");
        }

        message.append("<br><p>Thank you,</p><p>Paykesho Team</p>");
        return message.toString();
    }

    // Generate SMS content based on voucher action type
    private String generateSmsMessage(Transaction transaction, String actionType) {
        String txnAmount = "K" + transaction.getAmount();
        if ("PAYMENT".equals(actionType)) {
            return String.format("Dear %s, your payment request processed successfully. Amount: %s. Merchant: %s. Reference: %s. Thank you. Get lelo, Pay Kesho.", transaction.getClient(), txnAmount, transaction.getMerchant().getName(), transaction.getReference());
        }
        return "Unknown action type.";
    }
}
