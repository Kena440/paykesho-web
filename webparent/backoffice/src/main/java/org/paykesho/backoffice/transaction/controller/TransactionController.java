package org.paykesho.backoffice.transaction.controller;

import org.commons.entity.Transaction;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.transaction.service.TransactionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String showClientTransactions(Model model, HttpServletRequest request) {
        List<Transaction> transactionList = transactionsService.listAll();
        loadCurrencySetting(request);
        model.addAttribute("transactionList", transactionList);
        model.addAttribute("pageTitle", "All Transactions");

        return "transactions/transactions";
    }

    @GetMapping("/merchant/{id}")
    public String listAllTransactionsByMerchant(@PathVariable("id") int id, Model model) throws PaykeshoError {

        List<Transaction> transactionList = transactionsService.getMerchantTransactions(id);
        model.addAttribute("transactionList", transactionList);
        //todo
        model.addAttribute("totalItems", transactionList.size());
        model.addAttribute("currentPage", 1);
        model.addAttribute("previousPage", 1);
        model.addAttribute("nextPage", 1);
        model.addAttribute("startCount", 1);
        model.addAttribute("endCount", 1);
        model.addAttribute("totalPages", 1);
        return "transactions/transactions";
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
