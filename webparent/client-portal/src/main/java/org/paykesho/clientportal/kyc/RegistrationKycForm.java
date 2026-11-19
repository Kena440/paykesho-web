package org.paykesho.clientportal.kyc;

import lombok.Getter;
import lombok.Setter;

/**
 * Registration fields kept for KYC review that are not columns of app.clients
 * (PAYKESHO REGISTRATION: first/middle/last name and house number, road, area).
 * The client's name and address are composed from these on the server.
 */
@Getter
@Setter
public class RegistrationKycForm {
    private String firstName;
    private String middleName;
    private String lastName;
    private String houseNumber;
    private String road;
    private String area;
}
