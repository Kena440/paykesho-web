package org.paykesho.backoffice.organizations.controller;

import org.commons.entity.State;
import org.commons.entity.client.Client;
import org.commons.entity.client.EmployeeRecord;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.organizations.enums.Constants;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listAllOrganizationsByPage(Model model) throws PaykeshoError {
        model.addAttribute("pageTitle", "List Organizations");
        model.addAttribute("organizationList", organizationService.listAll());
        return "organizations/organizations";
    }

    @GetMapping("/new")
    public String newOrganization(Model model) {
        String pageTitle = "New Organization";
        Organization organization = new Organization();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        organization.setEnabled(true);

        model.addAttribute("organization", organization);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "organizations/organization_form";
    }

    @PostMapping("/save")
    public String saveOrganization(Organization organization, RedirectAttributes redirectAttributes) throws PaykeshoError {
        organization.setDistrict("N/A");
        organization.setRegistrationDate(new Date());
        organizationService.save(organization);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Operation Successful: " + organization.getEmail());
        return "redirect:/organizations";
       // return getRedirectURLtoAffectedOrganization(organization);
    }

    @GetMapping("/view/{id}")
    public String viewOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_ORGANIZATIONS;
        }
        String pageTitle = "View Organization";

        try {

            Organization organization = organizationService.getOrganizationById(id);
            List<EmployeeRecord> employeeRecords = null;
            List<Client> clientRecordsSize = null;
            OrganizationAdmin organizationAdmin = organization.getContactPerson();
            if (organizationAdmin == null) organizationAdmin = new OrganizationAdmin();

            if (organization != null) {
                employeeRecords = organizationService.listEmployeeRecordsAll(organization.getId());
                clientRecordsSize = organizationService.listClientRecordsAll(organization.getId());
            }

            loadCurrencySetting(request);
            model.addAttribute("organization", organization);
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("organizationAdmin", organizationAdmin);

            if (employeeRecords != null) model.addAttribute("employeeRecords", employeeRecords);
            if (employeeRecords != null) model.addAttribute("employeeRecordsSize", employeeRecords.size());
            if (employeeRecords != null) model.addAttribute("clientRecordsSize", clientRecordsSize.size());

            return "organizations/organization_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_ORGANIZATIONS;
        }
    }

    private String getRedirectURLtoAffectedOrganization(Organization organization) throws PaykeshoError {
        if (organization == null) {
            throw new PaykeshoError("no member provided");
        }
        String firstPartOfEmail = organization.getName();

        if (organization.getEmail() == null) {
            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization.getId();
        } else {
            return Constants.REDIRECT_TO_ORGANIZATIONS_KEYWORD + firstPartOfEmail;
        }
    }

    @GetMapping("/edit/{id}")
    public String editOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_ORGANIZATIONS;
        }
        String pageTitle = "Edit Organization";

        try {
            Organization organization = organizationService.getOrganizationById(id);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("organization", organization);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "organizations/organization_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_ORGANIZATIONS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            organizationService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_ORGANIZATIONS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateOrganizationEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            Organization organization = organizationService.getOrganizationById(id);
            organizationService.updateOrganizationStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = organization.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_ORGANIZATIONS;

    }

    @GetMapping("/details/{id}")
    public String viewOrganizationDetails(@PathVariable("id") Integer id, Model model) throws PaykeshoError {

        Organization organization = organizationService.getOrganizationById(id);
        model.addAttribute("organization", organization);
        return "organizations/organization_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportOrganizationsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError {
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
                throw new PaykeshoError("no format provided");
        }
    }

    @PostMapping("/employee-record/save")
    public String saveEmployeeRecord(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {

        String organizationIdParam = request.getParameter("organization");
        if (organizationIdParam == null || organizationIdParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization ID is required.");
            return null;
        }

        int organizationId;
        try {
            organizationId = Integer.parseInt(organizationIdParam);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Invalid Organization ID.");
            return null;
        }

        Organization organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
            return null;
        }

        EmployeeRecord employeeRecord = new EmployeeRecord();
        employeeRecord.setName(request.getParameter("name"));
        employeeRecord.setNrc(request.getParameter("nrc"));
        employeeRecord.setTpin(request.getParameter("tpin"));
        employeeRecord.setManNumber(request.getParameter("manNumber"));
        employeeRecord.setOrganization(organization);
        employeeRecord.setRegistrationDate(new Date());

        try {
            organizationService.saveEmployeeRecord(employeeRecord);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Operation Successful: " + organization.getEmail());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error saving employee record: " + e.getMessage());
            return null;
        }

        return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization.getId();
    }


    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
