package org.paykesho.paymentservice.payments.dto.card;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.commons.dto.CardPaymentRequest;
import org.paykesho.paymentservice.utils.PaymentConfigProperties;

@Data
public class CardPaymentDTO {
    private Auth auth;
    private Data data;

    public CardPaymentDTO(PaymentConfigProperties paymentConfig, CardPaymentRequest request) {
        this.auth = new Auth(paymentConfig);
        this.data = new Data(request);
    }


    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class Auth {
        @JsonProperty("merchant_id")
        private String merchantId;

        @JsonProperty("api_id")
        private String apiId;

        @JsonProperty("api_key")
        private String apiKey;

        @JsonProperty("channel")
        private String channel = "card";

        public Auth(PaymentConfigProperties paymentConfig) {
            this.merchantId = paymentConfig.getMerchantId();
            this.apiId = paymentConfig.getApiId();
            this.apiKey = paymentConfig.getApiKey();
        }
    }


    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class Data {
        @JsonProperty("method")
        private String method = "runTranAuthCapture";

        @JsonProperty("reference_no")
        private String referenceNo;

        @JsonProperty("amount")
        private String amount;

        @JsonProperty("description")
        private String description;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("address")
        private String address;
        @JsonProperty("email")
        private String email;
        @JsonProperty("phone")
        private String phone;
        @JsonProperty("city")
        private String city;
        @JsonProperty("state")
        private String state;
        @JsonProperty("currency")
        private String currency;
        @JsonProperty("zip_code")
        private String zipCode;
        @JsonProperty("country")
        private String country;

        public Data(CardPaymentRequest request) {
            this.referenceNo = request.getReferenceNo();
            this.amount = request.getAmount();
            this.description = request.getDescription();
            this.firstName = request.getFirstName();
            this.lastName = request.getLastName();
            this.address = request.getAddress();
            this.email = request.getEmail();
            this.phone = request.getPhone();
            this.city = request.getCity();
            this.state = request.getState();
            this.currency = request.getCurrency();
            this.zipCode = request.getZipCode();
            this.country = request.getCountry();

        }
    }


}