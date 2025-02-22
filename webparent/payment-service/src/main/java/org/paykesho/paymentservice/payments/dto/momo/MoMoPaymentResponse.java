package org.paykesho.paymentservice.payments.dto.momo;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoMoPaymentResponse {

    @JsonProperty("response")
    private Response response;

    @Data
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Response {

        @JsonProperty("response_code")
        private String responseCode;

        @JsonProperty("response_description")
        private String responseDescription;

        @JsonProperty("reference_no")
        private String referenceNo;

        @JsonProperty("transaction_id")
        private Long transactionId;

        @JsonProperty("operatorreference")
        private Long operatorReference;

        @JsonProperty("sender_id")
        private String senderId;

        @JsonProperty("operator")
        private String operator;

        @JsonProperty("transaction_date")
        private String transactionDate;
    }
}
