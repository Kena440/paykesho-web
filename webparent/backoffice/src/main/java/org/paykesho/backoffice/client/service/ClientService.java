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
import org.springframework.beans.factory.annotation.Autowired;
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
        helper.listEntities(pageNum, CUSTOMERS_PER_PAGE, clientRepository);
    }

    public List<Client> listAll() {
        List<Client> clientList = (List<Client>) clientRepository.findAll(Sort.by("code").ascending());
        for (Client c : clientList) {
            ClientBalance clientBalance = clientBalanceRepository.getClientBalanceByClientId(c.getId());
            c.setClientBalance(clientBalance);
            c.setCreditScore(creditScoreService.calculateCreditScore(c));
        }

        return clientList;
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        clientRepository.updateEnabledStatus(id, enabled);
    }

    public Client get(Integer id) throws PaykeshoError {
        try {
            return clientRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
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
