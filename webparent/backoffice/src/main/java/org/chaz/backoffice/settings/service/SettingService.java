package org.chaz.backoffice.settings.service;

import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.repository.*;
import org.commons.entity.State;
import org.commons.entity.facilities.LevelOfCare;
import org.commons.entity.settings.Country;
import org.commons.entity.settings.Currency;
import org.commons.entity.settings.GeneralSettingBag;
import org.commons.entity.settings.Setting;
import org.commons.enums.SettingCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private LevelOfCareRepository levelOfCareRepository;

    public List<Setting> settingsList() {
        return (List<Setting>) settingRepository.findAll();
    }

    public GeneralSettingBag getGeneralSettings() {
        List<Setting> settings = new ArrayList<>();
        List<Setting> generalSettings = settingRepository.findByCategory(SettingCategory.GENERAL);
        List<Setting> currencySettings = settingRepository.findByCategory(SettingCategory.CURRENCY);
        settings.addAll(generalSettings);
        settings.addAll(currencySettings);
        return new GeneralSettingBag(settings);
    }

    public void saveAll(Iterable<Setting> settings) {
        settingRepository.saveAll(settings);
    }

    public List<Setting> getMailServerSettings() {
        return settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
    }

    public List<Setting> getMailServerTemplatesSettings() {
        return settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES);
    }

    public EmailSettingBag getEmailSettingsBag() {
        List<Setting> settingList = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settingList.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new EmailSettingBag(settingList);
    }

    public List<Setting> getCurrencySettings() {
        return settingRepository.findByCategory(SettingCategory.CURRENCY);
    }

    public List<Setting> getPaymentSettings() {
        return settingRepository.findByCategory(SettingCategory.PAYMENT);
    }

    public String getCurrencyCode() {
        Setting setting = settingRepository.findByKey("CURRENCY_ID");
        Integer currencyId = Integer.parseInt(setting.getValue());
        Currency currency = currencyRepository.findById(currencyId).get();

        return currency.getCode();
    }



    public List<LevelOfCare> levelOfCareList() {
        return levelOfCareRepository.findAllByOrderByNameAsc();
    }

    public LevelOfCare findLevelOfCareById(Integer id) {
        return levelOfCareRepository.findById(id).get();
    }

    public List<Currency> currencyList() {
        return currencyRepository.findAllByOrderByNameAsc();
    }

    public List<State> listStatesByCountry(String country) {
        Country countryInDb = countryRepository.findCountryByName(country);
        if (countryInDb == null) {
            return null;
        }
        return stateRepository.findByCountryOrderByNameAsc(countryInDb);
    }

    public Country getCountryByName(String name) {
        Country countryInDb = countryRepository.findCountryByName(name);
        if (countryInDb == null) {
            return null;
        }
        return countryInDb;
    }

    public State getProvinceByName(String attendeeProvince) {
        State stateInDb = stateRepository.findById(Integer.valueOf(attendeeProvince)).get();
        if (stateInDb == null) {
            return null;
        }
        return stateInDb;
    }
}
