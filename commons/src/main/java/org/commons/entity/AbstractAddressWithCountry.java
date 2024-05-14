package org.commons.entity;

import lombok.Data;
import org.commons.entity.settings.Country;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@Data
@MappedSuperclass
public abstract class AbstractAddressWithCountry extends AbstractAddress {
    @ManyToOne
    @JoinColumn(name = "country_id")
    protected Country country;

    @Override
    public String toString() {
        String address = firstName;

        if (lastName != null && !lastName.isEmpty()) address += " " + lastName;
        if (!addressLine1.isEmpty()) address += ", \n" + addressLine1;
        if (addressLine2 != null && !addressLine2.isEmpty()) address += ", \n" + addressLine2;
        if (!city.isEmpty()) address += ", \n" + city;
        if (state != null && !state.isEmpty()) address += ", \n" + state;
        address += ", \n" + country.getName();
        if (!postalCode.isEmpty()) address += "\n. Postal Code: " + postalCode;
        if (!phoneNumber.isEmpty()) address += "\n. Phone: " + phoneNumber;

        return address;
    }
}
