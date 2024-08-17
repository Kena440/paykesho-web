package org.paykesho.backoffice.client.controller;

import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.enums.Constants;
import org.paykesho.backoffice.client.service.ClientService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService clientService;

    @GetMapping("")
    public String listFirstPage(Model model) {
        return Constants.REDIRECT_CUSTOMERS_PAGE_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listByPage(Model model,
                             @PathVariable(name = "pageNum") int pageNum,
                             @PagingAndSortingParam(listName = "clientList", moduleURL = "/clients") PagingAndSortingHelper helper) throws PaykeshoError {

        clientService.listByPage(pageNum, helper);

        return "clients/clients";
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateCustomerEnabledStatus(@PathVariable(name = "id") Integer id,
                                              @PathVariable(name = "status") boolean enabled,
                                              RedirectAttributes redirectAttributes,
                                              Model model) throws PaykeshoError {

        Client client = clientService.get(id);
        clientService.updateCustomerEnabledStatus(id, enabled);
        String status = enabled ? "enabled" : "has been disabled";
        String operationMSG = client.getFullName() + " has been " + status;
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        return Constants.REDIRECT_CUSTOMERS;

    }

    @GetMapping("/details/{id}")
    public String viewCustomer(@PathVariable("id") Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) throws PaykeshoError {
        try {
            Client client = clientService.get(id);
            model.addAttribute("customer", client);
            return "clients/client_details_modal";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute("message", paykeshoError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }
    }

    @GetMapping("/edit/{id}")
    public String editCustomer(@PathVariable(name = "id") Integer id,
                               RedirectAttributes redirectAttributes,
                               Model model) throws PaykeshoError {
        if (id == null) {
            return Constants.REDIRECT_CUSTOMERS;
        }
        String pageTitle = "Edit Client";

        try {
            Client client = clientService.get(id);
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
    public String saveCustomer(Client client,
                               Model model,
                               RedirectAttributes redirectAttributes) throws IOException, PaykeshoError {

        clientService.save(client);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "client Operation Successful: " + client.getFullName());
        return Constants.REDIRECT_CUSTOMERS;
    }

    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable(name = "id") Integer id,
                                 RedirectAttributes redirectAttributes,
                                 Model model) throws PaykeshoError {
        if (id == null) {
            return Constants.REDIRECT_CUSTOMERS;
        }

        try {
            clientService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "CUSTOMER deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_CUSTOMERS;
    }

}
