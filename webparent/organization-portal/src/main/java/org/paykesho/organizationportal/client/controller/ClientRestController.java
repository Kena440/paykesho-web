package org.paykesho.organizationportal.client.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.service.ClientService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/clients/rest")
public class ClientRestController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private VouchersService vouchersService;

    @PostMapping("/check-client")
    public String checkClientEmail(Integer id, String email) {
        return clientService.isEmailUnique(id, email) ? "OK" : "Duplicated";
    }

    @PostMapping("/balance")
    public ResponseEntity<ClientBalance> checkClientBalance(@RequestParam Integer clientId) throws PaykeshoError {
        Client client = clientService.get(clientId);
        ClientBalance balance = clientService.getClientBalance(client);
        return new ResponseEntity<>(balance, HttpStatus.OK);
    }

    @PostMapping("/voucher-totals")
    public ResponseEntity<BigDecimal> checkClientVoucherTotals(@RequestParam Integer clientId) throws PaykeshoError {
        Client client = clientService.get(clientId);
        BigDecimal totals = vouchersService.getApprovedAndValidClientVouchers(client).stream()
                .map(Voucher::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ;
        return new ResponseEntity<>(totals, HttpStatus.OK);
    }
}
