package org.paykesho.paymentservice.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfigProperties {
    private String merchantId;
    private String apiId;
    private String apiKey;
    private String url;
}
