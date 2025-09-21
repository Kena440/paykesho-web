package org.paykesho.merchantportal.transactions.controller;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.paykesho.merchantportal.security.MerchantAdminUserDetails;
import org.paykesho.merchantportal.transactions.service.TransactionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private MerchantService merchantService;
    @Autowired
    private MerchantAdminService merchantAdminService;

    @Autowired
    private TransactionsService transactionsService;

    @GetMapping("")
    public String showMerchantTransactions(@AuthenticationPrincipal MerchantAdminUserDetails loggedInUser, Model model) throws PaykeshoError {
        MerchantAdmin merchantAdmin = merchantAdminService.getMerchantAdminByEmail(loggedInUser.getMerchantAdmin().getEmail());
        Merchant merchant = merchantService.getMerchantByContactPerson(merchantAdmin);

        if (merchant == null) {
            // Handle case where merchant is not found
            model.addAttribute("transactionList", List.of());
            model.addAttribute("pageTitle", "Transactions");
            return "transactions/transactions";
        }

        List<Transaction> transactionList = transactionsService.getMerchantTransactions(merchant);
        model.addAttribute("transactionList", transactionList);
        model.addAttribute("pageTitle", merchant.getName() + " Transactions");

        return "transactions/transactions";
    }

}
