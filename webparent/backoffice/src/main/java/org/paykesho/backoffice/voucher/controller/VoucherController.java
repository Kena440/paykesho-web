package org.paykesho.backoffice.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.config.Utility;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.paykesho.backoffice.settings.email.EmailSettingBag;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.user.service.UserService;
import org.paykesho.backoffice.voucher.enums.Constants;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserService userService;

    @Autowired
    private SettingService settingService;
/*    @Autowired
    private SMSNotificationService smsNotificationService;*/

    @GetMapping("")
    public String listFirstPage(Model model) {
        return Constants.REDIRECT_TO_VOUCHERS_WITHOUT_KEYWORD;
    }

    @GetMapping("/pending")
    public String listPendingFirstPage(Model model) {
        return Constants.REDIRECT_TO_VOUCHERS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllVouchersByPage(
            @PathVariable("pageNum") int pageNum,
            @PagingAndSortingParam(listName = "voucherList", moduleURL = "/vouchers") PagingAndSortingHelper helper) throws PaykeshoError {
        voucherService.listByPage(pageNum, helper);

        return "vouchers/vouchers";
    }

    @GetMapping("/merchant/{id}")
    public String listAllVouchersByMerchant(
            @PathVariable("id") int id, Model model) throws PaykeshoError {

        // List<Voucher> voucherList = voucherService.getMerchantVouchers(id);

        // model.addAttribute("voucherList", voucherList);

        //   model.addAttribute("totalItems", voucherList.size());
        model.addAttribute("currentPage", 1);
        model.addAttribute("previousPage", 1);
        model.addAttribute("nextPage", 1);
        model.addAttribute("startCount", 1);
        model.addAttribute("endCount", 1);
        model.addAttribute("totalPages", 1);
        return "vouchers/vouchers";
    }

    @GetMapping("/view/{id}")
    public String showVoucherViewModal(@PathVariable("id") Integer id,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        Voucher voucher = voucherService.getVoucherById(id);
        model.addAttribute("voucher", voucher);
        return "vouchers/voucher_view_modal";
    }

    @GetMapping("/approve/{id}")
    public String showVoucherApprovalModal(@PathVariable("id") Integer id,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        Voucher voucher = voucherService.getVoucherById(id);
        model.addAttribute("voucher", voucher);
        return "vouchers/voucher_details_modal";
    }

    @GetMapping("/adjust/{id}")
    public String showVoucherAdjustmentModal(@PathVariable("id") Integer id,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {
        Voucher voucher = voucherService.getVoucherById(id);
        model.addAttribute("voucher", voucher);
        return "vouchers/voucher_adjustment_modal";
    }

    @PostMapping("/approve-voucher")
    public String approveVoucherRequest(HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {

        Voucher approvedVoucher = runVoucherApprovalProcess(request);

        redirectAttributes.addFlashAttribute("message", "Voucher has been approved #" + approvedVoucher.getReference());
        return "redirect:/vouchers";
    }

    @PostMapping("/adjust-voucher")
    public String adjustVoucherRequest(HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {

        Voucher adjustedVoucher = runVoucherAdjustmentProcess(request);

        redirectAttributes.addFlashAttribute("message", "Voucher has been adjusted #" + adjustedVoucher.getReference());
        return "redirect:/vouchers";
    }

    @GetMapping("/reject/{id}")
    public String rejectVoucherRequest(@PathVariable("id") Integer voucherId,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {

        Voucher rejectedVoucher = runVoucherRejectionProcess(voucherId, request);

        redirectAttributes.addFlashAttribute("message", "Voucher has been rejected #" + rejectedVoucher.getReference());
        return "redirect:/vouchers";
    }

    private Voucher runVoucherRejectionProcess(Integer voucherId, HttpServletRequest request) throws PaykeshoError {
        Voucher voucher = voucherService.getVoucherById(voucherId);

        if (voucher == null) {
            throw new PaykeshoError("no voucher found");
        }

        Voucher rejectedVoucher = voucherService.save(voucher);

        try {
            sendNotifications(rejectedVoucher, "REJECTION", null);

        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rejectedVoucher;
    }

    private Voucher runVoucherApprovalProcess(HttpServletRequest request) throws PaykeshoError {

        int voucherId = Integer.parseInt(request.getParameter("id"));
        String voucherRepaymentCycle = request.getParameter("voucherRepaymentCycle");

        Voucher voucher = voucherService.getVoucherById(voucherId);

        if (voucher == null) {
            throw new PaykeshoError("no voucher found");
        }
        voucher.setInterestPercent(Float.parseFloat(request.getParameter("voucherInterestPercent")));

        Date repaymentDate = getRepaymentDate(voucherRepaymentCycle, voucher);

        BigDecimal totalRepaymentAmount = getApprovalTotal(voucher).setScale(2, RoundingMode.CEILING);

        voucher.setRepaymentCycle(voucherRepaymentCycle);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setRepaymentDate(repaymentDate);
        voucher.setApproved(true);

        Voucher approvedVoucher = voucherService.save(voucher);

        try {
            sendNotifications(approvedVoucher, "APPROVAL", null);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return voucher;
    }

    private Voucher runVoucherAdjustmentProcess(HttpServletRequest request) throws PaykeshoError {

        int voucherId = Integer.parseInt(request.getParameter("id"));

        String voucherRepaymentCycle = request.getParameter("voucherRepaymentCycle");

        Voucher voucher = voucherService.getVoucherById(voucherId);

        if (voucher == null) {
            throw new PaykeshoError("no voucher found");
        }

        voucher.setInterestPercent(Float.parseFloat(request.getParameter("voucherInterestPercent")));

        Date repaymentDate = getRepaymentDate(voucherRepaymentCycle, voucher);

        BigDecimal voucherAmount = BigDecimal.valueOf(Long.parseLong(request.getParameter("voucherAdjustedAmount")));

        BigDecimal totalRepaymentAmount = getAdjustmentTotal(request).setScale(2, RoundingMode.CEILING);

        voucher.setRepaymentCycle(voucherRepaymentCycle);
        voucher.setAvailableAmount(voucherAmount);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setRepaymentDate(repaymentDate);
        voucher.setApproved(true);

        Voucher adjustedVoucher = voucherService.save(voucher);

        try {
            sendNotifications(adjustedVoucher, "ADJUSTMENT", voucherAmount);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return adjustedVoucher;
    }

    private void sendNotifications(Voucher voucher, String actionType, BigDecimal voucherAmount) throws MessagingException, UnsupportedEncodingException {

        String emailMessage = "";
        String emailSubject = "";
        String smsMessage = "";

        switch (actionType) {
            case "APPROVAL":
                emailSubject = "Voucher Request Approval #" + voucher.getReference();
                emailMessage = "<p>Congratulations!</p>" +
                        "<p>You requested for a voucher, which has been adjusted to K\"" + voucherAmount + "\". Voucher Reference: \"" + voucher.getReference() + "\"</p>" +
                        "<p>Kindly note that your repayment amount is K\"" + voucher.getRepaymentAmount() + "\" and your repayment date is  \"" + voucher.getRepaymentDate() + "\"</p>" +
                        "<br>" +
                        "<p>Get lelo, Pay Mailo. </p>";

                smsMessage = "Get lelo, Pay Mailo!. " +
                        "\nVoucher request approved. " +
                        "Note that your repayment amount is K\"" + voucher.getRepaymentAmount() + "\" to be paid on \"" + voucher.getRepaymentDate() + "\" \n" +
                        "Kindly proceed with payment of goods and services. # @REF".replace("@REF", voucher.getReference());

                break;
            case "REJECTION":
                emailSubject = "Voucher Request Rejected #" + voucher.getReference();
                emailMessage = "<p>Congratulations!</p>" +
                        "<p>You requested for a voucher which has been rejected. Voucher Reference: \"" + voucher.getReference() + "\"</p>" +
                        "<br>" +
                        "<p>Get lelo, Pay Mailo. </p>";

                smsMessage = "Get lelo, Pay Mailo!. " +
                        "\nVoucher request rejected. # @REF".replace("@REF", voucher.getReference());

                break;
            case "ADJUSTMENT":
                emailSubject = "Voucher Request Adjusted #" + voucher.getReference();
                emailMessage = "<p>Congratulations!</p>" +
                        "<p>You requested for a voucher, which has been adjusted to K\"" + voucherAmount + "\". Voucher Reference: \"" + voucher.getReference() + "\"</p>" +
                        "<p>Kindly note that your repayment amount is K\"" + voucher.getRepaymentAmount() + "\" and your repayment date is  \"" + voucher.getRepaymentDate() + "\"</p>" +
                        "<br>" +
                        "<p>Get lelo, Pay Mailo. </p>";

                smsMessage = "Get lelo, Pay Mailo!. " +
                        "\nVoucher request adjusted and approved. " +
                        "Note that your repayment amount is K\"" + voucher.getRepaymentAmount() + "\" to be paid on \"" + voucher.getRepaymentDate() + "\" \n" +
                        "Kindly proceed with payment of goods and services from. # @REF".replace("@REF", voucher.getReference());

                break;
            default:
                break;

        }

        //smsNotificationService.send(new SMSDto(voucher.getClient().getPhoneNumber(), smsMessage));
        sendVoucherEmail(voucher.getClient().getEmail(), emailMessage, emailSubject);
    }

    private BigDecimal getApprovalTotal(Voucher voucher) {
        BigDecimal voucherAmount = voucher.getAmount();
        BigDecimal interestAmount = BigDecimal.valueOf(voucher.getInterestPercent() / 100).multiply(voucherAmount);
        return voucherAmount.add(interestAmount);
    }

    private BigDecimal getAdjustmentTotal(HttpServletRequest request) {

        BigDecimal voucherAmount = BigDecimal.valueOf(Long.parseLong(request.getParameter("voucherAdjustedAmount")));
        float voucherInterestPercent = Float.parseFloat(request.getParameter("voucherInterestPercent"));

        BigDecimal interestAmount = BigDecimal.valueOf(voucherInterestPercent / 100).multiply(voucherAmount);
        return voucherAmount.add(interestAmount);
    }

    private Date getRepaymentDate(String voucherRepaymentCycle, Voucher voucher) {
        Date date = voucher.getRequestDate();
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        myCal.add(Calendar.MONTH, +Integer.parseInt(voucherRepaymentCycle));
        date = myCal.getTime();
        return date;
    }

    private void sendVoucherEmail(String email, String voucherMessage, String voucherSubject) throws MessagingException, UnsupportedEncodingException {

        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = email;
        String subject = voucherSubject;
        String content = voucherMessage;

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

}
