package org.paykesho.paymentservice.payments.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPaymentCallBackResponse {

    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("response_description")
    private String responseDescription;

    @JsonProperty("operator_reference")
    private String operatorReference;

    @JsonProperty("reference_no")
    private String referenceNo;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("birthday")
    private String birthday;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("sender_id")
    private String senderId;
}
