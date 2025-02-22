package org.paykesho.paymentservice.payments.dto.bank;


import lombok.Data;

@Data
public class BankPaymentResponse {
    private String response_code;
    private String response_description;
    private String operatorreference;
    private String reference_no;
    private String accountname;
    private String birthday;
    private String currency;
    private String date;
    private String merchant_callback_url;
    private String amount;
    private String receiver_id;
    private String operator;
    private String transaction_date;
    private String notification_type;
}
