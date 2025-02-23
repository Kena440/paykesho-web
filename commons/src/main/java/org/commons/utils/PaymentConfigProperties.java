package org.commons.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
public class PaymentConfigProperties {
    private String merchantId = "MEC00136";
    private String apiId = "ebb36796-f5c0-4f5a-ad3c-d192293b79cc";
    private String apiKey = "9acd66e1-5a17-46ee-a004-0e3bf27cab55";
    private String url = "https://sandbox.zynlepay.com/zynlepay/jsonapi";
}
