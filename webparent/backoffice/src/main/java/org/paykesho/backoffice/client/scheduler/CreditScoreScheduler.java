package org.paykesho.backoffice.client.scheduler;

import org.commons.entity.client.Client;
import org.paykesho.backoffice.client.service.ClientService;
import org.paykesho.backoffice.client.service.CreditScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreditScoreScheduler {

    @Autowired
    private ClientService clientService;
    @Autowired
    private CreditScoreService creditScoreService;

    @Scheduled(cron = "0 0 1 * * ?") // Runs every day at 1 AM
    public void updateAllClientCreditScores() {
        List<Client> clients = clientService.listAll();
        for (Client client : clients) {
            creditScoreService.updateCreditScore(client);
        }
    }

}
