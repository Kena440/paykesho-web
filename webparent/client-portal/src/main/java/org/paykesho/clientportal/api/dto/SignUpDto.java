package org.paykesho.clientportal.api.dto;

import lombok.Data;

@Data
public class SignUpDto {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String corporate;
}
