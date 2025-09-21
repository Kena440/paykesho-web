package org.paykesho.backoffice.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.settings.Country;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.repository.ClientBalanceRepository;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.repository.CountryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClientService {
    public static final int CUSTOMERS_PER_PAGE = 10;

    private final PasswordEncoder passwordEncoder;

    private final ClientRepository clientRepository;

    private final CountryRepository countryRepository;

    private final ClientBalanceRepository clientBalanceRepository;

    private final CreditScoreService creditScoreService;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        try {
            log.debug("Listing clients by page: {}", pageNum);
            helper.listEntities(pageNum, CUSTOMERS_PER_PAGE, clientRepository);
            log.debug("Successfully listed clients for page: {}", pageNum);
        } catch (PaykeshoError ex) {
            log.warn("PaykeshoError listing clients by page {}: {}", pageNum, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error listing clients by page {}: {}", pageNum, ex.getMessage(), ex);
            throw new RuntimeException("Failed to list clients by page", ex);
        }
    }

    public List<Client> listAll() {
        try {
            log.debug("Retrieving all clients");
            List<Client> clientList = (List<Client>) clientRepository.findAll(Sort.by("code").ascending());
            log.debug("Found {} clients, enriching with balance and credit score data", clientList.size());

            for (Client c : clientList) {
                try {
                    ClientBalance clientBalance = clientBalanceRepository.getClientBalanceByClientId(c.getId());
                    c.setClientBalance(clientBalance);
                } catch (Exception ex) {
                    log.warn("Failed to get balance for client ID {}: {}", c.getId(), ex.getMessage());
                    c.setClientBalance(null);
                }

                try {
                    int creditScore = creditScoreService.calculateCreditScore(c);
                    c.setCreditScore(creditScore);
                } catch (Exception ex) {
                    log.warn("Failed to calculate credit score for client ID {}: {}", c.getId(), ex.getMessage());
                    c.setCreditScore(0);
                }
            }

            log.debug("Successfully retrieved and enriched {} clients", clientList.size());
            return clientList;
        } catch (Exception ex) {
            log.error("Error retrieving all clients: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve all clients", ex);
        }
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        try {
            log.debug("Updating customer enabled status for ID: {} to: {}", id, enabled);
            clientRepository.updateEnabledStatus(id, enabled);
            log.debug("Successfully updated customer enabled status for ID: {}", id);
        } catch (Exception ex) {
            log.error("Error updating customer enabled status for ID {}: {}", id, ex.getMessage(), ex);
            throw new RuntimeException("Failed to update customer enabled status", ex);
        }
    }

    public Client get(Integer id) throws PaykeshoError {
        try {
            log.debug("Retrieving client by ID: {}", id);
            Client client = clientRepository.findById(id).get();
            log.debug("Found client with ID: {}", id);
            return client;
        } catch (Exception exception) {
            log.warn("Client not found with ID: {}", id);
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        try {
            log.debug("Retrieving country list");
            List<Country> countries = countryRepository.findAllByOrderByNameDesc();
            log.debug("Successfully retrieved {} countries", countries.size());
            return countries;
        } catch (Exception ex) {
            log.error("Error retrieving country list: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve country list", ex);
        }
    }

    public boolean isEmailUnique(Integer id, String email) {
        Client existingCustomer = clientRepository.findByEmail(email);

        if (existingCustomer != null && existingCustomer.getId() != id) {
            return false;
        }

        return true;
    }

    public void save(Client customerInForm) {
        Client customerInDB = clientRepository.findById(customerInForm.getId()).get();

        if (!customerInForm.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(customerInForm.getPassword());
            customerInForm.setPassword(encodedPassword);
        } else {
            customerInForm.setPassword(customerInDB.getPassword());
        }

        customerInForm.setEnabled(customerInDB.isEnabled());
        customerInForm.setCreatedTime(customerInDB.getCreatedTime());
        customerInForm.setVerificationCode(customerInDB.getVerificationCode());
        clientRepository.save(customerInForm);
    }

    public Long countAllClients() {
        return clientRepository.countAllClients();
    }

    public ClientBalance saveClientBalance(ClientBalance clientBalance) {
        return clientBalanceRepository.save(clientBalance);
    }

    public ClientBalance getClientBalance(Client client) {
        return clientBalanceRepository.getClientBalanceByClientId(client.getId());
    }
}
