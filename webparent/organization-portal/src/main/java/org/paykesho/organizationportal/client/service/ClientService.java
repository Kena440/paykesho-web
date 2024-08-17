package org.paykesho.organizationportal.client.service;

import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.repository.ClientRepository;
import org.paykesho.organizationportal.paging.PagingAndSortingHelper;
import org.paykesho.organizationportal.settings.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class ClientService {
    public static final int CUSTOMERS_PER_PAGE = 10;

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, CUSTOMERS_PER_PAGE, clientRepository);
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

    public void delete(Integer id) throws PaykeshoError {
        Long count = clientRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("could not find client");
        }
        clientRepository.deleteById(id);
    }

    public Long countAllClients() {
        return clientRepository.countAllClients();
    }
}
