package org.commons.dto;

import lombok.Data;

@Data
public class CardDepositRequest {
    private String api_key;
    private String api_id;
    private String sender_id;
    private String merchant_id;
    private String channel;
    private String description;
    private String reference_no;
    private int method;
    private int amount;
    private String first_name;
    private String last_name;
    private String address;
    private String email;
    private String phone;
    private String city;
    private String state;
    private String currency;
    private String zip_code;
    private String country;
}
