package org.paykesho.paymentservice.payments.dto.momo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.paykesho.paymentservice.utils.PaymentConfigProperties;

@Data
public class MoMoPaymentDTO {

    private Auth auth;
    private Data data;

    public MoMoPaymentDTO(PaymentConfigProperties paymentConfig, MoMoPaymentRequest request) {
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
        private String channel = "momo";

        public Auth(PaymentConfigProperties paymentConfig) {
            this.merchantId = paymentConfig.getMerchantId();
            this.apiId = paymentConfig.getApiId();
            this.apiKey = paymentConfig.getApiKey();
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class Data {
        @JsonProperty("method")
        private String method = "runPayToEwallet";

        @JsonProperty("reference_no")
        private String referenceNo;

        @JsonProperty("amount")
        private String amount;

        @JsonProperty("receiver_id")
        private String receiverId;

        public Data(MoMoPaymentRequest request) {
            this.referenceNo = request.getReferenceNo();
            this.amount = request.getAmount();
            this.receiverId = request.getReceiverId();
        }
    }
}
