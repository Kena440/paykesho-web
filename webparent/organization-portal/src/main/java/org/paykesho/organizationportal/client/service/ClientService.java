package org.paykesho.organizationportal.client.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

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

    // UUID-based methods
    public Client getByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            log.error("getByUuid called with null or empty UUID");
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.error("getByUuid called with invalid UUID format: {}", uuid);
            throw new PaykeshoError("Invalid UUID format");
        }

        return clientRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaykeshoError("Could not find client with UUID: " + uuid));
    }

    public void updateClientEnabledStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        Client client = getByUuid(uuid);
        updateClientEnabledStatus(client.getId(), enabled);
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

    public boolean isEmailUniqueByUuid(String uuid, String email) {
        // If creating new (no UUID), just check if email exists
        if (uuid == null || uuid.trim().isEmpty()) {
            Client existingClient = clientRepository.findByEmail(email);
            return existingClient == null;
        }

        // If editing, get the current client and exclude it from the check
        try {
            Client currentClient = clientRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            return isEmailUnique(currentClient.getId(), email);
        } catch (Exception e) {
            // If client not found, treat as new
            Client existingClient = clientRepository.findByEmail(email);
            return existingClient == null;
        }
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
