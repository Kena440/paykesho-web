package org.commons.dto;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPaymentInitialResponse {

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

        @JsonProperty("redirect_url")
        private String redirectUrl;

        @JsonProperty("amount")
        private String amount;

        @JsonProperty("udf1")
        private String udf1;

        @JsonProperty("udf2")
        private String udf2;

        @JsonProperty("udf3")
        private String udf3;

        @JsonProperty("udf4")
        private String udf4;

        @JsonProperty("udf5")
        private String udf5;
    }
}
