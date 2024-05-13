package org.chaz.backoffice.facilities.controller;

import org.chaz.membershipbackoffice.donations.service.DonationService;
import org.chaz.membershipbackoffice.finance.service.MemberBalanceService;
import org.chaz.membershipbackoffice.initiatives.service.InitiativeService;
import org.chaz.membershipbackoffice.memberadmin.service.MemberAdminService;
import org.chaz.membershipbackoffice.memberinstitution.enums.Constants;
import org.chaz.membershipbackoffice.memberinstitution.service.MemberInstitutionService;
import org.chaz.membershipbackoffice.paging.PagingAndSortingHelper;
import org.chaz.membershipbackoffice.paging.PagingAndSortingParam;
import org.chaz.membershipbackoffice.settings.service.SettingService;
import org.chaz.membershipbackoffice.utils.AmazonS3Util;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.donations.MemberInstitutionDonation;
import org.membershipcommons.entity.finance.MemberInstitutionBalance;
import org.membershipcommons.entity.initiative.Initiative;
import org.membershipcommons.entity.initiative.InitiativeMember;
import org.membershipcommons.entity.members.Denomination;
import org.membershipcommons.entity.members.LevelOfCare;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.entity.settings.FundingProject;
import org.membershipcommons.entity.settings.NeedType;
import org.membershipcommons.entity.settings.Setting;
import org.membershipcommons.utils.MembershipMgtError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/member-institutions")
public class MemberInstitutionController {

    @Autowired
    private MemberInstitutionService memberInstitutionService;
    @Autowired
    private MemberAdminService memberAdminService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MemberBalanceService memberBalanceService;
    @Autowired
    private DonationService donationService;
    @Autowired
    private InitiativeService initiativeService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_MEMBERS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllMemberInstitutionsByPage(@PagingAndSortingParam(listName = "memberList", moduleURL = "/member-institutions") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum, Model model) throws MembershipMgtError {
        memberInstitutionService.listByPage(pageNum, helper);

        return "memberinstitutions/member_institutions";
    }

    @GetMapping("/new")
    public String newMemberInstitution(Model model) {
        String pageTitle = "New Member Institution";
        MemberInstitution memberInstitution = new MemberInstitution();
        List<Denomination> denominationList = settingService.denominationList();
        List<LevelOfCare> levelOfCareList = settingService.levelOfCareList();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        memberInstitution.setEnabled(true);
        memberInstitution.setYearOpened(new Date());

        model.addAttribute("memberInstitution", memberInstitution);
        model.addAttribute("denominationList", denominationList);
        model.addAttribute("levelOfCareList", levelOfCareList);
        model.addAttribute("memberAdmin", null);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "memberinstitutions/member_form";
    }

    @PostMapping("/save")
    public String saveMemberInstitution(MemberInstitution memberInstitution, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws IOException, MembershipMgtError {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            memberInstitution.setYearOpened(dateFormatter.parse(memberInstitution.getYOOnForm()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            memberInstitution.setPhotos(fileName);
            MemberInstitution savedMemberInstitution = memberInstitutionService.save(memberInstitution);
            String uploadDir = "member_photos/" + savedMemberInstitution.getId();

            if (!fileName.isEmpty()) {
                AmazonS3Util.removeFolder(uploadDir);
                AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
            }
        } else {
            if (memberInstitution.getPhotos().isEmpty()) {
                memberInstitution.setPhotos(null);
            }
            memberInstitutionService.save(memberInstitution);
        }

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Member Institution Operation Successful: " + memberInstitution.getEmail());
        return getRedirectURLtoAffectedMemberInstitution(memberInstitution);
    }

    @GetMapping("/view/{id}")
    public String viewMemberInstitution(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) throws MembershipMgtError {
        if (id == null) {
            return Constants.REDIRECT_TO_MEMBERS;
        }
        String pageTitle = "View Member Institution";

        try {

            MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionById(id);
            MemberAdmin memberAdmin = memberInstitution.getContactPerson();

            if (memberAdmin == null) memberAdmin = new MemberAdmin();

            MemberInstitutionBalance memberInstitutionBalance = memberBalanceService.getMemberInstitutionBalanceByMember(memberInstitution);

            List<MemberInstitutionDonation> donationList = donationService.memberInstitutionDonations(memberInstitution.getId());
            List<NeedType> needTypeList = settingService.needTypeList();
            List<FundingProject> fundingProjectList = settingService.fundingProjectList();
            List<Initiative> memberInitiatives = new ArrayList<>();
            List<Initiative> initiatives = initiativeService.listAll();

            getInitiativesForMember(memberInstitution, memberInitiatives, initiatives);

            loadCurrencySetting(request);
            model.addAttribute("memberInstitution", memberInstitution);
            model.addAttribute("donationList", donationList);
            model.addAttribute("needTypeList", needTypeList);
            model.addAttribute("fundingProjectList", fundingProjectList);
            model.addAttribute("memberAdmin", memberAdmin);
            model.addAttribute("memberInstitutionBalance", memberInstitutionBalance);
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("memberInitiatives", memberInitiatives);

            return "memberinstitutions/member_institution_view";
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
            return Constants.REDIRECT_TO_MEMBERS;
        }
    }

    private void getInitiativesForMember(MemberInstitution memberInstitution, List<Initiative> memberInitiatives, List<Initiative> initiatives) {
        for (Initiative initiative : initiatives) {
            for (InitiativeMember initiativeMember : initiative.getInitiativeMembers()) {
                if (initiativeMember.getMemberInstitution() == memberInstitution) {
                    memberInitiatives.add(initiativeMember.getInitiative());
                }
            }
        }
    }


    private String getRedirectURLtoAffectedMemberInstitution(MemberInstitution memberInstitution) throws MembershipMgtError {
        if (memberInstitution == null) {
            throw new MembershipMgtError("no member provided");
        }
        String firstPartOfEmail = memberInstitution.getEmail().split("@")[0];

        if (memberInstitution.getContactPerson() == null) {
            return Constants.REDIRECT_TO_MEMBER + memberInstitution.getId();
        } else {
            return Constants.REDIRECT_TO_MEMBERS_KEYWORD + firstPartOfEmail;
        }
    }

    @GetMapping("/edit/{id}")
    public String editMemberInstitution(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws MembershipMgtError {
        if (id == null) {
            return Constants.REDIRECT_TO_MEMBERS;
        }
        String pageTitle = "Edit Member Institution";

        try {
            MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionById(id);
            MemberAdmin memberAdmin = memberInstitution.getContactPerson();

            if (memberAdmin == null) memberAdmin = new MemberAdmin();

            List<Denomination> denominationList = settingService.denominationList();
            List<LevelOfCare> levelOfCareList = settingService.levelOfCareList();
            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("memberInstitution", memberInstitution);
            model.addAttribute("memberAdmin", memberAdmin);
            model.addAttribute("denominationList", denominationList);
            model.addAttribute("levelOfCareList", levelOfCareList);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "memberinstitutions/member_form";
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
            return Constants.REDIRECT_TO_MEMBERS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteMemberInstitution(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws MembershipMgtError {
        if (id == null) {
            throw new MembershipMgtError("no parameter id");
        }

        try {
            memberInstitutionService.delete(id);

            String memberPhotosDir = "member_photos/" + id;
            AmazonS3Util.removeFolder(memberPhotosDir);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "MemberInstitution deleted successfully");
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
        }
        return Constants.REDIRECT_TO_MEMBERS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateMemberInstitutionEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws MembershipMgtError {

        if (id == null) {
            throw new MembershipMgtError("no parameter id");
        }

        try {
            MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionById(id);
            memberInstitutionService.updateMemberInstitutionStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = memberInstitution.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (MembershipMgtError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
        }
        return Constants.REDIRECT_TO_MEMBERS;

    }

    @GetMapping("/details/{id}")
    public String viewMemberInstitutionDetails(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) throws MembershipMgtError {

        MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionById(id);
        model.addAttribute("memberInstitution", memberInstitution);
        return "memberinstitutions/member_details_modal";

    }

    @PostMapping("/needs/save")
    public String saveNeeds(HttpServletRequest request, RedirectAttributes redirectAttributes) throws MembershipMgtError, MessagingException, UnsupportedEncodingException {

        MemberInstitution memberInstitution = memberInstitutionService.getMemberInstitutionById(Integer.valueOf(request.getParameter("memberInstitutionId")));
        String[] memberNeeds = request.getParameterValues("memberNeeds");

        if (memberNeeds.length > 0) {
            memberInstitution.getMemberNeeds().clear();

            for (String type : memberNeeds) {
                NeedType needType = settingService.findNeedTypeById(Integer.valueOf(type));
                memberInstitution.getMemberNeeds().add(needType);
            }

            if (memberInstitution != null) {
                memberInstitutionService.save(memberInstitution);
            } else {
                throw new MembershipMgtError("No institution found");
            }
        }

        redirectAttributes.addFlashAttribute(org.chaz.membershipbackoffice.memberadmin.enums.Constants.MESSAGE, "Institution Operation Successful: " + memberInstitution.getName());
        return Constants.REDIRECT_TO_MEMBER + memberInstitution.getId();

    }

    @GetMapping("/export/{format}")
    public void exportMemberInstitutionsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws MembershipMgtError, IOException {
        switch (format) {
            case "csv":
                //(response);
                break;
            case "excel":
                //exportToExcel(response);
                break;
            case "pdf":
                //exportToPDF(response);
                break;
            default:
                throw new MembershipMgtError("no format provided");
        }
    }

   /* public void exportToCSV(HttpServletResponse response) throws IOException {
        List<MemberInstitution> memberList = memberService.listAll();
        MemberInstitutionCSVExporter exporter = new MemberInstitutionCSVExporter();
        exporter.export(memberList, response);
    }

    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<MemberInstitution> memberList = memberService.listAll();
        MemberInstitutionExcelExporter exporter = new MemberInstitutionExcelExporter();
        exporter.export(memberList, response);
    }

    public void exportToPDF(HttpServletResponse response) throws IOException {
        List<MemberInstitution> memberList = memberService.listAll();
        MemberInstitutionPDFExporter exporter = new MemberInstitutionPDFExporter();
        exporter.export(memberList, response);
    }*/

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
