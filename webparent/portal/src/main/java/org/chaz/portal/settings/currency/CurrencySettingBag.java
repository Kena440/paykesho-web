package org.chaz.membershipsite.settings.currency;

import org.membershipcommons.entity.settings.Setting;
import org.membershipcommons.entity.settings.SettingBag;

import java.util.List;

public class CurrencySettingBag extends SettingBag {
    public CurrencySettingBag(List<Setting> settingList) {
        super(settingList);
    }

    public String getCURRENCY_SYMBOL() {
        return super.getValue("CURRENCY_SYMBOL");
    }

    public String getCURRENCY_SYMBOL_POSITION() {
        return super.getValue("CURRENCY_SYMBOL_POSITION");
    }

    public String getDECIMAL_POINT_TYPE() {
        return super.getValue("DECIMAL_POINT_TYPE");
    }

    public int getDECIMAL_DIGITS() {
        return Integer.parseInt(super.getValue("DECIMAL_DIGITS"));
    }

    public String getTHOUSAND_POINT_TYPE() {
        return super.getValue("THOUSAND_POINT_TYPE");
    }

}
