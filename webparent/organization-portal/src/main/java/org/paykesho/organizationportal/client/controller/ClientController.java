package org.paykesho.organizationportal.client.controller;

import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.entity.settings.Setting;
import org.commons.enums.ClientType;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.enums.Constants;
import org.paykesho.organizationportal.client.service.ClientService;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.organizations.service.OrganizationService;
import org.paykesho.organizationportal.security.OrganizationAdminUserDetails;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.paykesho.organizationportal.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private SettingService settingService;


    @GetMapping("")
    public String showClients(@AuthenticationPrincipal OrganizationAdminUserDetails loggedInUser, Model model, HttpServletRequest request) {
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(loggedInUser.getOrganizationAdmin().getEmail());
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
        List<Client> clientList = clientService.listAll(organization);
        loadCurrencySetting(request);
        model.addAttribute("clientList", clientList);
        model.addAttribute("pageTitle", "Clients");

        return "clients/clients";
    }

    @GetMapping("/{uuid}/enabled/{status}")
    public String updateClientEnabledStatus(@PathVariable(name = "uuid") String uuid, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) throws PaykeshoError {

        Client client = ownClient(uuid, request);
        clientService.updateClientEnabledStatusByUuid(uuid, enabled);
        String status = enabled ? "enabled" : "has been disabled";
        String operationMSG = client.getFullName() + " has been " + status;
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        return Constants.REDIRECT_CUSTOMERS;

    }

    @GetMapping("/new")
    public String newClient(Model model) {
        String pageTitle = "New Client";
        Client client = new Client();

        client.setEnabled(true);
        client.setAccountNonLocked(true);

        model.addAttribute("client", client);
        model.addAttribute("pageTitle", pageTitle);
        return "clients/client_form";
    }

    @GetMapping("/view/{uuid}")
    public String viewClient(@PathVariable("uuid") String uuid, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) throws PaykeshoError {
        try {
            Client client = ownClient(uuid, request);
            model.addAttribute("pageTitle", "Employee");
            model.addAttribute("customer", client);
            return "clients/client_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }
    }

    @GetMapping("/details/{uuid}")
    public String viewClientDetails(@PathVariable("uuid") String uuid, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) throws PaykeshoError {
        try {
            Client client = ownClient(uuid, request);
            model.addAttribute("customer", client);
            return "clients/client_details_modal";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }
    }

    @GetMapping("/edit/{uuid}")
    public String editClient(@PathVariable(name = "uuid") String uuid, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            return Constants.REDIRECT_CUSTOMERS;
        }
        String pageTitle = "Edit Client";

        try {
            Client client = ownClient(uuid, request);
            List<Country> countryList = clientService.countryList();

            model.addAttribute("client", client);
            model.addAttribute("countryList", countryList);
            model.addAttribute("pageTitle", pageTitle);

            return "clients/client_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }

    }

    @PostMapping("/save")
    public String saveClient(Client client, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) throws IOException, PaykeshoError {

        OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);

        // Only new clients can be saved here; an id in the form must not overwrite an existing client.
        if (client.getId() != null) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Existing clients cannot be changed from this form.");
            return Constants.REDIRECT_CUSTOMERS;
        }

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_CLT");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        client.setOrganization(organization);
        client.setCode(paykeshoRef);
        client.setCreatedTime(new Date());
        client.setClientType(ClientType.FORMAL);
        clientService.save(client);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "client Operation Successful: " + client.getFullName());
        return Constants.REDIRECT_CUSTOMERS;
    }

    /**
     * The client with this uuid, only if they are an employee of the signed-in
     * employer; otherwise "not found", so one employer cannot see or change
     * another employer's staff.
     */
    private Client ownClient(String uuid, HttpServletRequest request) throws PaykeshoError {
        Client client = clientService.getByUuid(uuid);
        OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
        if (client == null || organization == null || client.getOrganization() == null
                || !organization.getId().equals(client.getOrganization().getId())) {
            throw new PaykeshoError("Client not found.");
        }
        return client;
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
