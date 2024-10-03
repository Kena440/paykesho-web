package org.paykesho.clientportal.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherRequestDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.notifications.service.NotificationService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.settings.service.SettingService;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {
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
    /*    @Autowired
        private SMSNotificationService smsNotificationService;*/
    @Autowired
    NotificationService notificationService;

    @GetMapping("")
    public String showClientVouchers(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Voucher> voucherList = voucherService.getClientVouchers(client);
        model.addAttribute("client", client);
        model.addAttribute("voucherList", voucherList);
        model.addAttribute("pageTitle", "My Vouchers");

        return "vouchers/vouchers";
    }

    @GetMapping("/request")
    public String showClientVoucherRequestForm(Model model) {
        model.addAttribute("voucherRequest", new VoucherRequestDto());
        model.addAttribute("pageTitle", "Voucher Request");

        return "vouchers/request";
    }

    @PostMapping("/save")
    public String saveSubmittedRequest(VoucherRequestDto voucherRequestDto, @AuthenticationPrincipal ClientUserDetails loggedInUser, RedirectAttributes redirectAttributes, HttpServletRequest request) throws IOException, PaykeshoError, MessagingException {

        Voucher voucher = new Voucher();
        Voucher savedVoucher = null;
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_VCR");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        voucher.setReference(paykeshoRef);
        voucher.setApproved(false);
        voucher.setReason(voucherRequestDto.getReason());
        voucher.setClient(client);
        voucher.setAmount(voucherRequestDto.getAmount());
        voucher.setAvailableAmount(voucherRequestDto.getAmount());
        voucher.setRequestDate(new Date());
        savedVoucher = voucherService.save(voucher);
        /*if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            voucher.setPayslipFile(fileName);

            savedVoucher = voucherService.save(voucher);
            String uploadDir = "client_payslips/" + savedVoucher.getClient().getId();

            if (!fileName.isEmpty()) {
                AmazonS3Util.removeFolder(uploadDir);
                AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
            }
        } else {
            if (voucher.getPayslipFile().isEmpty()) {
                voucher.setPayslipFile(null);
            }
            savedVoucher = voucherService.save(voucher);
        }*/

        sendNotification(savedVoucher, null);

        redirectAttributes.addFlashAttribute("message", "Voucher request submitted. Kindly wait for processing. #" + paykeshoRef);

        return "redirect:/vouchers";
    }

    @GetMapping("/cancel")
    public String showClientVoucherCancellationForm(Model model) {

        //model.addAttribute("voucherRefDto", new VoucherRefDto());
        return "vouchers/voucher_cancellation";
    }

    private void sendNotification(Voucher voucher, BigDecimal voucherAmount) {
        String emailMessage = generateEmailMessage(voucher, "REQUEST", voucherAmount);
        String smsMessage = generateSmsMessage(voucher, "REQUEST");

        boolean emailSent = notificationService.sendNotification(
                voucher.getClient().getEmail(),
                emailMessage,
                NotificationType.email.defaultDescription()
        );

        if (emailSent) {
            System.out.println("Email sent successfully to " + voucher.getClient().getEmail());
        } else {
            System.err.println("Failed to send email to " + voucher.getClient().getEmail());
        }

        boolean smsSent = notificationService.sendNotification(
                voucher.getClient().getPhoneNumber(),
                smsMessage,
                NotificationType.sms.defaultDescription()
        );

        if (smsSent) {
            System.out.println("SMS sent successfully to " + voucher.getClient().getPhoneNumber());
        } else {
            System.err.println("Failed to send SMS to " + voucher.getClient().getPhoneNumber());
        }
    }

    private String generateEmailMessage(Voucher voucher, String actionType, BigDecimal voucherAmount) {
        StringBuilder message = new StringBuilder("<p>Congratulations!</p>");

        String repaymentAmount = "K" + voucher.getRepaymentAmount();
        //String repaymentDate = voucher.getRepaymentDate().toString();
        String repaymentDate = new Date().toString();
        String voucherReference = voucher.getReference();

        switch (actionType) {
            case "REQUEST":
                message.append("<p>Your voucher request has been submitted. ")
                        .append("Repayment amount: ").append(repaymentAmount)
                        .append(", Repayment date: ").append(repaymentDate)
                        .append(".</p>");
                break;
            default:
                message.append("<p>Unknown action type.</p>");
                break;
        }
        message.append("<br><p>Get lelo, Paykesho.</p>");
        return message.toString();
    }

    private String generateSmsMessage(Voucher voucher, String actionType) {
        String repaymentAmount = "K" + voucher.getRepaymentAmount();
        //String repaymentDate = voucher.getRepaymentDate().toString();
        String repaymentDate = new Date().toString();
        String voucherReference = voucher.getReference();

        switch (actionType) {
            case "REQUEST":
                return String.format("Voucher request has been submitted. Repayment amount: %s, to be paid on %s. Reference: %s. Please wait for approval processing. Get lelo, Pay Kesho.",
                        repaymentAmount, repaymentDate, voucherReference);
            default:
                return "Unknown action type.";
        }
    }


}
