package org.paykesho.clientportal.transactions.controller;

import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.MerchantDto;
import org.paykesho.clientportal.api.dto.PaymentDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.settings.email.EmailSettingBag;
import org.paykesho.clientportal.settings.service.SettingService;
import org.paykesho.clientportal.transactions.service.TransactionsService;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private SettingService settingService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private TransactionsService transactionsService;

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
        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_TRX");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        BigDecimal availableBalance = voucherService.getApprovedAndValidClientVouchersBalance(client);
        if (availableBalance == null || availableBalance.compareTo(amount) < 0) {
            throw new PaykeshoError("error processing payment. payment amount is larger or insufficient balance.");
        }

        BigDecimal newVoucherAvailableAmount = availableBalance.subtract(amount);

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setMerchant(merchantService.getMerchantByPaykeshoRef(merchantDto.getCode()));
        transaction.setAmount(amount);
        transaction.setReference(paykeshoRef);
        transaction.setNaration(paykeshoRef);
        transaction.setTxnDate(new Date());
        transaction.setSettled(false);

        // Uncomment if voucher update is necessary
        // Voucher updateVoucher = voucherService.updateVoucherAvailableAmount(client, newVoucherAvailableAmount);
        // transaction.setVoucher(updateVoucher);

        return transactionsService.save(transaction);
    }

    private void sendPaymentEmail(Transaction transaction) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = transaction.getClient().getEmail();
        String subject = "Paykesho Payment #" + transaction.getReference();
        String content = "<p>Hello, </p>" + "<p>You made a payment successfully of \"" + transaction.getAmount() + "\" to \"" + transaction.getMerchant().getName() + "\". Transaction Reference: \"" + transaction.getReference() + "\"</p>" + "<br>" + "<p>Get lelo, Pay Kesho. </p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }
}
