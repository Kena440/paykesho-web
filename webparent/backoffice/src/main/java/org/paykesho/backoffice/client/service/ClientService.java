package org.paykesho.backoffice.client.service;

import org.commons.entity.client.Client;
import org.commons.entity.settings.Country;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.repository.CountryRepository;
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

    public void delete(Integer id) throws PaykeshoError {
        Long count = clientRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("could not find customer");
        }
        clientRepository.deleteById(id);
    }

    public Long countAllClients() {
        return clientRepository.countAllClients();
    }
}
