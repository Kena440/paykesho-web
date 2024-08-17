package org.paykesho.organizationportal.utils;

import net.bytebuddy.utility.RandomString;
import org.paykesho.organizationportal.client.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;

@Service
public class PaykeshoReferenceService {


    @Autowired
    private ClientRepository clientRepository;


    public String generateCode(String itemRequest) {
        String reference = "";
        String itemPrefix = "";
        Long count;

        switch (itemRequest) {

            case "ITEM_REQ_CLT":
                itemPrefix = "C";
                count = clientRepository.countAllClients() + 1;
                reference = getReference(itemPrefix, count);
                break;

            default:

                break;
        }

        return reference;
    }

    private String getReference(String itemPrefix, Long count) {
        String reference;

        Date dt = new Date();
        int year = dt.getYear();
        int currentYear = year + 1900;

        String randomCode = RandomString.make(5).toUpperCase(Locale.ENGLISH);
        reference = itemPrefix + currentYear + "" + count + randomCode;
        return reference;
    }

}
