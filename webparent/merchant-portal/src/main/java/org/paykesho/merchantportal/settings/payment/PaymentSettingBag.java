package org.paykesho.merchantportal.settings.payment;

import org.commons.entity.settings.Setting;
import org.commons.entity.settings.SettingBag;

import java.util.List;

public class PaymentSettingBag extends SettingBag {
    public PaymentSettingBag(List<Setting> settingList) {
        super(settingList);
    }

    public String getAPI_BASE_URL() {
        return super.getValue("PAYPAL_API_BASE_URL");
    }

    public String getAPI_CLIENT_ID() {
        return super.getValue("PAYPAL_API_CLIENT_ID");
    }

    public String getAPI_CLIENT_SECRET() {
        return super.getValue("PAYPAL_API_CLIENT_SECRET");
    }

}
