package org.paykesho.backoffice.kyc;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.kyc.ClientKycApplication;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.security.MembershipMgtUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * KYC review queue: back-office staff check a new client's details against the
 * employer's staff list and the uploaded NRC/payslip, then approve (activates
 * the account), reject, or ask for more information.
 */
@Slf4j
@Controller
@RequestMapping("/kyc")
public class KycReviewController {

    private static final String REDIRECT_QUEUE = "redirect:/kyc";

    private final KycReviewService kycReviewService;

    public KycReviewController(KycReviewService kycReviewService) {
        this.kycReviewService = kycReviewService;
    }

    @GetMapping("")
    public String queue(Model model) {
        model.addAttribute("openList", kycReviewService.openApplications());
        model.addAttribute("decidedList", kycReviewService.decidedApplications());
        model.addAttribute("pageTitle", "KYC Review");
        return "kyc/kyc_queue";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClientKycApplication application = kycReviewService.get(id);
            model.addAttribute("application", application);
            model.addAttribute("client", application.getClient());
            model.addAttribute("documents", kycReviewService.documentsFor(id));
            model.addAttribute("staffRecord", kycReviewService.staffRecordFor(application.getClient()).orElse(null));
            model.addAttribute("pageTitle", "KYC Review");
            return "kyc/kyc_view";
        } catch (PaykeshoError e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return REDIRECT_QUEUE;
        }
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable("id") Long id, @AuthenticationPrincipal MembershipMgtUserDetails me,
                        RedirectAttributes redirectAttributes) {
        return act(id, redirectAttributes, () -> kycReviewService.markUnderReview(id, reviewerId(me)),
                "Marked as under review.", true);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id, @RequestParam(value = "note", required = false) String note,
                          @AuthenticationPrincipal MembershipMgtUserDetails me, RedirectAttributes redirectAttributes) {
        return act(id, redirectAttributes, () -> kycReviewService.approve(id, reviewerId(me), note),
                "Registration approved. The client can now sign in.", false);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id, @RequestParam(value = "reason", required = false) String reason,
                         @AuthenticationPrincipal MembershipMgtUserDetails me, RedirectAttributes redirectAttributes) {
        return act(id, redirectAttributes, () -> kycReviewService.reject(id, reviewerId(me), reason),
                "Registration rejected.", false);
    }

    @PostMapping("/{id}/more-info")
    public String moreInfo(@PathVariable("id") Long id, @RequestParam(value = "reason", required = false) String reason,
                           @AuthenticationPrincipal MembershipMgtUserDetails me, RedirectAttributes redirectAttributes) {
        return act(id, redirectAttributes, () -> kycReviewService.requestMoreInfo(id, reviewerId(me), reason),
                "More information requested from the client.", true);
    }

    private interface Action {
        ClientKycApplication run() throws PaykeshoError;
    }

    private String act(Long id, RedirectAttributes redirectAttributes, Action action, String success, boolean stay) {
        try {
            ClientKycApplication a = action.run();
            redirectAttributes.addFlashAttribute("message", success + " (" + a.getFullName() + ")");
            return stay ? "redirect:/kyc/" + id : REDIRECT_QUEUE;
        } catch (PaykeshoError e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/kyc/" + id;
        }
    }

    private static Integer reviewerId(MembershipMgtUserDetails me) {
        return me == null || me.getUser() == null ? null : me.getUser().getId();
    }
}
