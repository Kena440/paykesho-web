package org.paykesho.clientportal.transactions.controller;

import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
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
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
/*
    @Autowired
    private SMSNotificationService smsNotificationService;
*/

    @GetMapping("/client-transactions")
    public String showClientTransactions(@AuthenticationPrincipal ClientUserDetails loggedInUser,
                                         Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

        List<Transaction> transactionList = transactionsService.getClientTransactions(client);
        model.addAttribute("customer", client);
        model.addAttribute("transactionList", transactionList);
        return "transactions/transactions";
    }

    @GetMapping("/pay-merchant")
    public String showClientTransactionPaymentForm(
            @AuthenticationPrincipal ClientUserDetails loggedInUser,
            Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Voucher> voucherList = voucherService.getApprovedAndValidClientVouchers(client);
        model.addAttribute("voucherList", voucherList);
        // model.addAttribute("payment", new PaymentDto());
        return "merchants/pay_merchant";
    }

    @PostMapping("/submit-payment")
    public String saveSubmittedPayment(
            @AuthenticationPrincipal ClientUserDetails loggedInUser,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request) throws IOException, PaykeshoError, MessagingException {

        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

        Transaction trx = runPaymentSubmission(client, request);

        return "transactions/" + (!trx.getReference().isEmpty() ? "payment_success" : "payment_fail");
    }

    private Transaction runPaymentSubmission(Client client, HttpServletRequest request) throws PaykeshoError, MessagingException, UnsupportedEncodingException {

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_TRX");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        int voucherId = Integer.parseInt(request.getParameter("voucherAmount").split("-")[0]);
        String transactionNarration = request.getParameter("transactionNarration");

        Voucher voucher = voucherService.getVoucherById(voucherId);

        if (voucher == null) throw new PaykeshoError("voucher doesnt exist");

        BigDecimal voucherAvailableAmount = voucher.getAvailableAmount();
        BigDecimal paymentAmount = BigDecimal.valueOf(Long.parseLong(request.getParameter("transactionAmount")));
        String meterAccountNumber = request.getParameter("meterAccountNumber");

        if (voucherAvailableAmount.compareTo(paymentAmount) == -1)
            throw new PaykeshoError("error processing payment. payment amount is larger");
        BigDecimal newVoucherAvailableAmount = voucherAvailableAmount.subtract(paymentAmount);

        voucher.setAvailableAmount(newVoucherAvailableAmount);
        Voucher updateVoucher = voucherService.save(voucher);

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        // transaction.setMerchant(voucher.getMerchant());
        transaction.setAmount(paymentAmount);
        transaction.setReference(paykeshoRef);
        transaction.setVoucher(updateVoucher);
        transaction.setTxnDate(new Date());
        transaction.setNaration(transactionNarration);
        // if (!meterAccountNumber.isEmpty()) transaction.setMeterAccountNumber(meterAccountNumber);

     /*   try {
            //sendNotifications(transaction, paykeshoRef);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        return (transactionsService.save(transaction));

    }

/*
    private void sendNotifications(Transaction transaction, String paykeshoRef) throws MessagingException, UnsupportedEncodingException {
        sendPaymentEmail(transaction);
        smsNotificationService.send(new SMSDto(transaction.getClient().getPhoneNumber(), (!transaction.getReference()().isEmpty()) ? "Payment successful. Kindly wait for processing. #" + paykeshoRef : "Payment failed. Please try again later"));
        smsNotificationService.send(new SMSDto(transaction.getMerchant().getPhoneNumber(), (!transaction.getReference()().isEmpty()) ? "Payment of goods received. Kindly note transaction id. #" + paykeshoRef : "Payment failed."));
    }
*/

    private void sendPaymentEmail(Transaction transaction) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = transaction.getClient().getEmail();
        String subject = "Paykesho Payment #" + transaction.getReference();
        String content = "<p>Hello, </p>" +
                "<p>You made a payment successfully of \"" + transaction.getAmount() + "\" to \"" + transaction.getMerchant().getName() + "\". Transaction Reference: \"" + transaction.getReference() + "\"</p>" +
                "<br>" +
                "<p>Get lelo, Pay Mailo. </p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }
}
