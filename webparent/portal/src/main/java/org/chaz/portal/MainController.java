package org.chaz.membershipsite;

import org.chaz.membershipsite.donations.service.DonationService;
import org.chaz.membershipsite.finance.service.InvoiceService;
import org.chaz.membershipsite.finance.service.MemberBalanceService;
import org.chaz.membershipsite.iga.service.IGAService;
import org.chaz.membershipsite.memberadmins.service.MemberAdminService;
import org.chaz.membershipsite.memberinstitution.service.MemberInstitutionService;
import org.chaz.membershipsite.settings.service.SettingService;
import org.membershipcommons.entity.finance.MemberInstitutionBalance;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.entity.settings.NeedType;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class MainController {

    @Autowired
    private MemberAdminService memberAdminService;
    @Autowired
    private MemberInstitutionService memberInstitutionService;
    @Autowired
    private MemberBalanceService memberBalanceService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private DonationService donationService;
    @Autowired
    private IGAService igaService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) throws MembershipMgtError {
        MemberAdmin memberAdmin = memberAdminService.getAuthenticatedMemberAdmin(request);
        MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionByMemberAdmin(memberAdmin);
        List<NeedType> needTypeList = settingService.needTypeList();

        MemberInstitutionBalance memberInstitutionBalance = memberBalanceService.getMemberInstitutionBalanceByMember(memberInstitution);
        long totalLoans = igaService.getTotalMemberIGALoans(memberInstitution.getId());
        Double totalOwing = igaService.getTotalOwingMemberIGALoans(memberInstitution.getId());

        model.addAttribute("countUnpaidInvoices", invoiceService.countTotalInvoices(memberInstitution.getId(), false));
        model.addAttribute("countTotalDonations", donationService.getCount(memberInstitution.getId()));
        model.addAttribute("memberInstitution", memberInstitution);
        model.addAttribute("totalOwing", totalOwing);
        model.addAttribute("totalLoans", totalLoans);
        model.addAttribute("needTypeList", needTypeList);
        if (memberInstitutionBalance != null)
            model.addAttribute("memberInstitutionBalance", memberInstitutionBalance.getBalance());

        return "index";
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "security/login";
        }
        return "redirect:/";
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        return "register/signup_home";
    }
}
