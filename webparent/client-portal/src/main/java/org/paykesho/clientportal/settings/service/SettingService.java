package org.paykesho.clientportal.settings.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.State;
import org.commons.entity.settings.Country;
import org.commons.entity.settings.Currency;
import org.commons.entity.settings.Setting;
import org.commons.enums.SettingCategory;
import org.paykesho.clientportal.settings.currency.CurrencySettingBag;
import org.paykesho.clientportal.settings.email.EmailSettingBag;
import org.paykesho.clientportal.settings.payment.PaymentSettingBag;
import org.paykesho.clientportal.settings.repository.CountryRepository;
import org.paykesho.clientportal.settings.repository.CurrencyRepository;
import org.paykesho.clientportal.settings.repository.SettingRepository;
import org.paykesho.clientportal.settings.repository.StateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SettingService {
    @Autowired
    private SettingRepository settingRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private CountryRepository countryRepository;

    public List<Setting> getGeneralSettings() {
        return settingRepository.findByTwoCategories(SettingCategory.GENERAL, SettingCategory.CURRENCY);
    }

    public EmailSettingBag getEmailSettingsBag() {
        List<Setting> settingList = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settingList.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new EmailSettingBag(settingList);
    }

    public CurrencySettingBag getCurrencySettingsBag() {
        List<Setting> settingList = settingRepository.findByCategory(SettingCategory.CURRENCY);
        settingList.addAll(settingRepository.findByCategory(SettingCategory.CURRENCY));

        return new CurrencySettingBag(settingList);
    }

    public PaymentSettingBag getPaymentSettingsBag() {
        List<Setting> settingList = settingRepository.findByCategory(SettingCategory.PAYMENT);
        settingList.addAll(settingRepository.findByCategory(SettingCategory.PAYMENT));

        return new PaymentSettingBag(settingList);
    }

    public String getCurrencyCode() {
        Setting setting = settingRepository.findByKey("CURRENCY_ID");
        Integer currencyId = Integer.parseInt(setting.getValue());
        Currency currency = currencyRepository.findById(currencyId).get();

        return currency.getCode();
    }

    public List<State> listStatesByCountry(Country country) {
        return stateRepository.findByCountryOrderByNameAsc(country);
    }

    public List<Setting> getCurrencySettings() {
        return settingRepository.findByCategory(SettingCategory.CURRENCY);
    }

    public Country getCountryByName(String name) {
        Country countryInDb = countryRepository.findCountryByName(name);
        if (countryInDb == null) {
            return null;
        }
        return countryInDb;
    }
}
