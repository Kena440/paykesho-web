package org.paykesho.clientportal.clients.service;


import net.bytebuddy.utility.RandomString;
import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.settings.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

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

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(String email) {
        Client client = clientRepository.findByEmail(email);
        return client == null;
    }

    public void registerClient(Client client) {
        encodePassword(client);
        client.setEnabled(false);
        client.setCreatedTime(new Date());
        client.setAuthenticationType(AuthenticationType.DATABASE);

        String randomCode = RandomString.make(64);
        client.setVerificationCode(randomCode);
        clientRepository.save(client);
    }

    private void encodePassword(Client client) {
        String encodedPassword = passwordEncoder.encode(client.getPassword());
        client.setPassword(encodedPassword);
    }

    public boolean verifyClient(String verificationCode) {
        Client client = clientRepository.findByVerificationCode(verificationCode);

        if (client == null || client.isEnabled()) {
            return false;
        } else {
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
        return clientRepository.findByEmail(email);
    }
    public Client getClientByCode(String code) {
        return clientRepository.findByCode(code);
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
        encodePassword(client);
        clientRepository.save(client);
    }

    public Client getAuthenticatedClient(HttpServletRequest request) {
        String email = Utility.getEmailOfAuthenticatedClient(request);
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
}
