package org.paykesho.clientportal.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherRequestDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.settings.email.EmailSettingBag;
import org.paykesho.clientportal.settings.service.SettingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {
    @Autowired
    private ClientService customerService;
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

    @GetMapping("/client-vouchers")
    public String showClientVouchers(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = customerService.getClientByCode(loggedInUser.getClient().getCode());

        List<Voucher> voucherList = voucherService.getClientVouchers(client);
        model.addAttribute("customer", client);
        model.addAttribute("voucherList", voucherList);
        return "vouchers/vouchers";
    }

    @GetMapping("/request")
    public String showClientVoucherRequestForm(Model model) {

        List<Merchant> merchantList = merchantService.listAllEnabledMerchants();
        model.addAttribute("merchantList", merchantList);
        model.addAttribute("voucherRequest", new VoucherRequestDto());
        return "vouchers/voucher_request";
    }

    @PostMapping("/submit-request")
    public String saveSubmittedRequest(VoucherRequestDto voucherRequestDto, @AuthenticationPrincipal ClientUserDetails loggedInUser, RedirectAttributes redirectAttributes, HttpServletRequest request, @RequestParam("image") MultipartFile multipartFile) throws IOException, PaykeshoError, MessagingException {

        Voucher voucher = new Voucher();
        Voucher savedVoucher = null;
        Client client = customerService.getClientByCode(loggedInUser.getClient().getCode());

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_VCR");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        voucher.setReference(paykeshoRef);
        voucher.setApproved(false);
        // voucher.setMerchant(voucherRequestDto.getMerchant());
        voucher.setClient(client);
        voucher.setAmount(voucherRequestDto.getVoucherAmount());
        voucher.setAvailableAmount(voucherRequestDto.getVoucherAmount());
        voucher.setRequestDate(new Date());

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

        sendNotifications(request, savedVoucher, paykeshoRef);

        redirectAttributes.addFlashAttribute("message", "Voucher request submitted. Kindly wait for processing. #" + paykeshoRef);

        return "redirect:/vouchers/client-vouchers";
    }

    private void sendNotifications(HttpServletRequest request, Voucher voucher, String paykeshoRef) throws MessagingException, UnsupportedEncodingException {
        sendVoucherEmail(voucher);
        //smsNotificationService.send(new SMSDto(voucher.getClient().getPhoneNumber(), "Voucher request submitted. Kindly wait for processing. #" + paykeshoRef));
    }

    @GetMapping("/cancel")
    public String showClientVoucherCancellationForm(Model model) {

        //model.addAttribute("voucherRefDto", new VoucherRefDto());
        return "vouchers/voucher_cancellation";
    }

    private void sendVoucherEmail(Voucher voucher) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = voucher.getClient().getEmail();
        String subject = "Voucher Request";
        String content = "<p>Hello, </p>" + "<p>You requested for a voucher. Voucher Reference: \"" + voucher.getReference() + "\"</p>" + "<p>Kindly wait for processing</>" + "<br>" + "<p>Get lelo, Pay Mailo. </p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }
}
