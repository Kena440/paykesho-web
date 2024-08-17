package org.paykesho.backoffice.transaction.controller;

import org.commons.entity.Transaction;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.paykesho.backoffice.transaction.enums.Constants;
import org.paykesho.backoffice.transaction.service.TransactionsService;
import org.paykesho.backoffice.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private UserService userService;

    @GetMapping("")
    public String listFirstPage(Model model) {
        return Constants.REDIRECT_TO_TRANSACTIONS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllTransactionsByPage(
            @PathVariable("pageNum") int pageNum,
            @PagingAndSortingParam(listName = "transactionList", moduleURL = "/transactions") PagingAndSortingHelper helper) throws PaykeshoError {
        transactionsService.listByPage(pageNum, helper);

        return "transactions/transactions";
    }

    @GetMapping("/merchant/{id}")
    public String listAllVouchersByMerchant(
            @PathVariable("id") int id, Model model) throws PaykeshoError {

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
}
