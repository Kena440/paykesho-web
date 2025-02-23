package org.commons.dto;

import lombok.Data;

@Data
public class CardPaymentRequest {
    private String referenceNo;
    private String amount;
    private String description;
    private String firstName;
    private String lastName;
    private String address;
    private String email;
    private String phone;
    private String city;
    private String state;
    private String currency;
    private String zipCode;
    private String country;
}
