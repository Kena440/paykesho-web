package org.paykesho.clientportal.clients.service;


import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.security.SecurityValidator;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.repository.ClientBalanceRepository;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.paykesho.clientportal.clients.repository.EmployeeRecordRepository;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.settings.repository.CountryRepository;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ClientService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EmployeeRecordRepository employeeRecordRepository;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private ClientBalanceRepository clientBalanceRepository;

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

    public boolean isEmailUnique(String email) {
        try {
            log.debug("Checking email uniqueness for: {}", email);
            Client client = clientRepository.findByEmail(email);
            boolean isUnique = client == null;
            log.debug("Email {} is unique: {}", email, isUnique);
            return isUnique;
        } catch (Exception ex) {
            log.error("Error checking email uniqueness for {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to check email uniqueness", ex);
        }
    }

    public void registerClient(Client client) {
        try {
            log.debug("Registering new client with email: {}", client.getEmail());
            
            // SECURITY: Validate and sanitize all client input
            try {
                SecurityValidator.validateAndSanitizeClient(client);
            } catch (SecurityException se) {
                log.error("SECURITY VIOLATION during client registration: {}", se.getMessage());
                throw new RuntimeException("Invalid input: Malicious content detected", se);
            }
            
            encodePassword(client);
            client.setEnabled(true);
            client.setAccountNonLocked(true);
            client.setCreatedTime(new Date());
            client.setAuthenticationType(AuthenticationType.DATABASE);

            String randomCode = RandomString.make(64);
            client.setVerificationCode(randomCode);
            Client savedClient = clientRepository.save(client);
            createClientBalance(savedClient);
            log.debug("Successfully registered client with ID: {}", savedClient.getId());
        } catch (Exception ex) {
            log.error("Error registering client with email {}: {}", client.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to register client", ex);
        }
    }

    private void createClientBalance(Client client) {
        ClientBalance balance = getClientBalance(client);
        if (balance == null) {
            balance = new ClientBalance();
            balance.setClient(client);
            balance.setCurrentBalance(BigDecimal.ZERO);
            balance.setAvailableBalance(BigDecimal.ZERO);
            balance.setHeldBalance(BigDecimal.ZERO);
            balance.setLastUpdate(new Date());
            saveClientBalance(balance);
        } else {
            log.warn("ClientBalance already exists for client id: {}. Skipping creation.", client.getId());
        }
    }

    private void encodePassword(Client client) {
        String encodedPassword = passwordEncoder.encode(client.getPassword());
        client.setPassword(encodedPassword);
    }

    public boolean verifyClient(String verificationCode) throws PaykeshoError {
        Client client = clientRepository.findByVerificationCode(verificationCode);
        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_CLT");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        if (client == null || client.isEnabled()) {
            return false;
        } else {
            client.setCode(paykeshoRef);
            clientRepository.enable(client.getId());
            return true;
        }
    }

    public void updateAuthenticationType(Client client, AuthenticationType authenticationType) {
        if (!client.getAuthenticationType().equals(authenticationType)) {
            clientRepository.updateAuthenticationType(client.getId(), authenticationType);
        }
    }

    public Client getClientByEmail(String email) {
        try {
            log.debug("Retrieving client by email: {}", email);
            Client client = clientRepository.findByEmail(email);
            if (client != null) {
                log.debug("Found client with ID: {}", client.getId());
            } else {
                log.debug("No client found with email: {}", email);
            }
            return client;
        } catch (Exception ex) {
            log.error("Error retrieving client by email {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve client by email", ex);
        }
    }

    public Client getClientByCode(String code) {
        try {
            log.debug("Retrieving client by code: {}", code);
            Client client = clientRepository.findByCode(code);
            if (client != null) {
                log.debug("Found client with ID: {}", client.getId());
            } else {
                log.debug("No client found with code: {}", code);
            }
            return client;
        } catch (Exception ex) {
            log.error("Error retrieving client by code {}: {}", code, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve client by code", ex);
        }
    }

    public Client getClientByResetToken(String token) {
        return clientRepository.findByResetToken(token);
    }


    public void addNewClientUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        Client client = new Client();
        client.setEmail(email);
        setName(name, client);

        client.setName(name);
        client.setEnabled(true);
        client.setCreatedTime(new Date());
        client.setAuthenticationType(authenticationType);
        client.setPassword("");

        clientRepository.save(client);
    }

    private void setName(String name, Client client) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            client.setName(name);
        } else {
            String firstName = nameArray[0];
            client.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            csoAdmin.setLastName(lastName);*/
        }
    }

    public void updateClient(Client clientInForm) {
        try {
            log.debug("Updating client with ID: {}", clientInForm.getId());
            
            // SECURITY: Validate and sanitize all client input
            try {
                SecurityValidator.validateAndSanitizeClient(clientInForm);
            } catch (SecurityException se) {
                log.error("SECURITY VIOLATION during client update: {}", se.getMessage());
                throw new RuntimeException("Invalid input: Malicious content detected", se);
            }
            
            Client clientInDB = clientRepository.findById(clientInForm.getId()).get();

            if (clientInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
                if (!clientInForm.getPassword().isEmpty()) {
                    String encodedPassword = passwordEncoder.encode(clientInForm.getPassword());
                    clientInForm.setPassword(encodedPassword);
                } else {
                    clientInForm.setPassword(clientInDB.getPassword());
                }
            } else {
                clientInForm.setPassword(clientInDB.getPassword());
            }

            clientInForm.setSalutation(clientInDB.getSalutation());
            clientInForm.setEnabled(clientInDB.isEnabled());
            clientInForm.setCreatedTime(clientInDB.getCreatedTime());
            clientInForm.setVerificationCode(clientInDB.getVerificationCode());
            clientInForm.setAuthenticationType(clientInDB.getAuthenticationType());
            clientInForm.setResetPasswordToken(clientInDB.getResetPasswordToken());
            clientRepository.save(clientInForm);
            log.debug("Successfully updated client with ID: {}", clientInForm.getId());
        } catch (Exception ex) {
            log.error("Error updating client with ID {}: {}", clientInForm.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to update client", ex);
        }
    }

    public String updateClientResetToken(String email) throws PaykeshoError {
        Client client = clientRepository.findByEmail(email);

        if (client != null) {
            String token = RandomString.make(30);
            client.setResetPasswordToken(token);
            clientRepository.save(client);

            return token;
        } else {
            throw new PaykeshoError("customer not found");
        }
    }

    public void updatePassword(String token, String newPassword) throws PaykeshoError {
        Client client = clientRepository.findByResetToken(token);
        if (client == null) {
            throw new PaykeshoError("no customer found. invalid token");
        }

        client.setPassword(newPassword);
        client.setResetPasswordToken(null);
        client.setEnabled(true); // Enable account after successful password reset
        encodePassword(client);
        clientRepository.save(client);
    }

    public Client getAuthenticatedClient(HttpServletRequest request) {
        String email = Utility.getEKeshofAuthenticatedClient(request);
        return getClientByEmail(email);
    }

    public void increaseFailedAttempts(Client client) {
        int newFailAttempts = client.getFailedAttempt() + 1;
        clientRepository.updateFailedAttempts(newFailAttempts, client.getEmail());
    }

    public void resetFailedAttempts(String email) {
        clientRepository.updateFailedAttempts(0, email);
    }

    public void lock(Client client) {
        client.setAccountNonLocked(false);
        client.setLockTime(new Date());

        clientRepository.save(client);
    }

    public boolean unlockWhenTimeExpired(Client client) {
        long lockTimeInMillis = client.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            client.setAccountNonLocked(true);
            client.setLockTime(null);
            client.setFailedAttempt(0);

            clientRepository.save(client);

            return true;
        }

        return false;
    }

    public String checkClientStatus(String name, String nrc, String tpin, String manNumber, Organization organization) {
        try {
            log.debug("Checking client status for name: {}, nrc: {}, organization: {}", name, nrc, organization.getId());
            String resp = "";

            boolean clientExists = clientRepository.existsByNrcAndTpinAndManNumberAndOrganization(nrc, tpin, manNumber, organization);
            if (clientExists) {
                resp = "Duplicated";
                log.debug("Client status: Duplicated");
                return resp;
            }

            boolean employeeExists = employeeRecordRepository.existsByNameAndNrcAndTpinAndManNumberAndOrganization(name, nrc, tpin, manNumber, organization);
            if (!employeeExists) {
                resp = "Not Found";
            } else {
                resp = "OK";
            }

            log.debug("Client status: {}", resp);
            return resp;
        } catch (Exception ex) {
            log.error("Error checking client status for name: {}, nrc: {}: {}", name, nrc, ex.getMessage(), ex);
            throw new RuntimeException("Failed to check client status", ex);
        }
    }

    public ClientBalance saveClientBalance(ClientBalance clientBalance) {
        return clientBalanceRepository.save(clientBalance);
    }

    public ClientBalance getClientBalance(Client client) {
        try {
            log.debug("Retrieving client balance for client ID: {}", client.getId());
            ClientBalance balance = clientBalanceRepository.getClientBalanceByClientId(client.getId());
            if (balance != null) {
                log.debug("Found client balance for client ID: {}", client.getId());
            } else {
                log.warn("No ClientBalance found for client id: {}. Returning null.", client.getId());
            }
            return balance;
        } catch (Exception ex) {
            log.error("Error retrieving client balance for client ID {}: {}", client.getId(), ex.getMessage(), ex);
            log.warn("No ClientBalance found for client id: {}. Returning null.", client.getId());
            return null;
        }
    }

    // UUID-based methods for API endpoints
    public Optional<Client> getClientById(Integer id) {
        try {
            return clientRepository.findById(id);
        } catch (Exception ex) {
            log.error("Error retrieving client by ID: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    public Client getClientByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new PaykeshoError("Invalid UUID format");
        }

        return clientRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaykeshoError("Could not find client with UUID: " + uuid));
    }
}
