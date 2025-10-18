package org.paykesho.backoffice.client.controller;

import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.enums.Constants;
import org.paykesho.backoffice.client.service.ClientService;
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
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String showClients(Model model, HttpServletRequest request) {
        List<Client> clientList = clientService.listAll();
        loadCurrencySetting(request);
        model.addAttribute("clientList", clientList);
        model.addAttribute("pageTitle", "Clients");

        return "clients/clients";
    }

    @GetMapping("/{uuid}/enabled/{status}")
    public String updateCustomerEnabledStatus(@PathVariable(name = "uuid") String uuid,
                                              @PathVariable(name = "status") boolean enabled,
                                              RedirectAttributes redirectAttributes,
                                              Model model) throws PaykeshoError {

        Client client = clientService.getByUuid(uuid);
        clientService.updateCustomerEnabledStatusByUuid(uuid, enabled);
        String status = enabled ? "enabled" : "has been disabled";
        String operationMSG = client.getFullName() + " has been " + status;
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        return Constants.REDIRECT_CUSTOMERS;

    }

    @GetMapping("/details/{uuid}")
    public String viewCustomer(@PathVariable("uuid") String uuid,
                               Model model,
                               RedirectAttributes redirectAttributes) throws PaykeshoError {
        try {
            Client client = clientService.getByUuid(uuid);
            model.addAttribute("customer", client);
            return "clients/client_details_modal";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }
    }

    @GetMapping("/edit/{uuid}")
    public String editCustomer(@PathVariable(name = "uuid") String uuid,
                               RedirectAttributes redirectAttributes,
                               Model model) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            return Constants.REDIRECT_CUSTOMERS;
        }
        String pageTitle = "Edit Client";

        try {
            Client client = clientService.getByUuid(uuid);
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
    public String saveCustomer(@Valid Client client,
                               Model model,
                               RedirectAttributes redirectAttributes) throws IOException, PaykeshoError {

        try {
            clientService.save(client);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "client Operation Successful: " + client.getFullName());
        } catch (Exception ex) {
            String errorMessage = "Failed to save client. Please check your input.";
            if (ex.getMessage() != null && ex.getMessage().contains("Malicious content detected")) {
                errorMessage = "Invalid input detected. Please ensure all fields contain valid data.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
        
        return Constants.REDIRECT_CUSTOMERS;
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
