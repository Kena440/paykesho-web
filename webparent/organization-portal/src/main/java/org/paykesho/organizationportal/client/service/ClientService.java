package org.paykesho.organizationportal.client.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.commons.enums.PaymentStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.repository.ClientBalanceRepository;
import org.paykesho.organizationportal.client.repository.ClientRepository;
import org.paykesho.organizationportal.settings.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class ClientService {
    public static final int CUSTOMERS_PER_PAGE = 10;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private ClientBalanceRepository clientBalanceRepository;

    public List<Client> listAll(Organization organization) {
        List<Client> clientList = clientRepository.getOrganizationClients(organization);
        for (Client c : clientList) {
            ClientBalance clientBalance = clientBalanceRepository.getClientBalanceByClientId(c.getId());
            c.setClientBalance(clientBalance);
        }

        return clientList;
    }

    public void updateClientEnabledStatus(Integer id, boolean enabled) {
        clientRepository.updateEnabledStatus(id, enabled);
    }

    public Client get(Integer id) throws PaykeshoError {
        try {
            return clientRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no client found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(Integer id, String email) {
        Client existingClient = clientRepository.findByEmail(email);

        if (existingClient != null && existingClient.getId() != id) {
            return false;
        }

        return true;
    }

    public void save(Client clientInForm) {
        Client clientInDB = clientRepository.findById(clientInForm.getId()).get();

        if (!clientInForm.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(clientInForm.getPassword());
            clientInForm.setPassword(encodedPassword);
        } else {
            clientInForm.setPassword(clientInDB.getPassword());
        }

        clientInForm.setEnabled(clientInDB.isEnabled());
        clientInForm.setCreatedTime(clientInDB.getCreatedTime());
        clientInForm.setVerificationCode(clientInDB.getVerificationCode());
        clientRepository.save(clientInForm);
    }

    public Long countAllClients() {
        return clientRepository.countAllClients();
    }

    public ClientBalance getClientBalance(Client client) {
        return clientBalanceRepository.getClientBalanceByClientId(client.getId());
    }

    public List<Client> listAllWithRepaymentSchedules(Organization organization, PaymentStatus paymentStatus) {
        List<Client> clientList = clientRepository.getOrganizationClients(organization);
        for (Client c : clientList) {
            ClientBalance clientBalance = clientBalanceRepository.getClientBalanceByClientId(c.getId());
            c.setClientBalance(clientBalance);
        }

        return clientList;
    }
}
