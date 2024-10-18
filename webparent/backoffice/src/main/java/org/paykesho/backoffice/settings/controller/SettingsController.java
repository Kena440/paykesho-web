package org.paykesho.backoffice.settings.controller;

import org.commons.constants.Constants;
import org.commons.entity.settings.Currency;
import org.commons.entity.settings.GeneralSettingBag;
import org.commons.entity.settings.Setting;
import org.paykesho.backoffice.settings.repository.CurrencyRepository;
import org.paykesho.backoffice.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private SettingService settingService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @GetMapping("")
    public String listAll(Model model) {
        List<Setting> settingList = settingService.settingsList();
        List<Currency> currencyList = currencyRepository.findAllByOrderByNameAsc();
        model.addAttribute("currencyList", currencyList);
        model.addAttribute("pageTitle", "Settings");

        for (Setting setting : settingList) {
            model.addAttribute(setting.getKey(), setting.getValue());
        }

        model.addAttribute("S3_BASE_URI", Constants.S3_BASE_URI);

        return "settings/settings";
    }

    @PostMapping("/save-general")
    public String saveGeneralSettings(@RequestParam("fileImage") MultipartFile multipartFile, HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        GeneralSettingBag settingBag = settingService.getGeneralSettings();
        saveSiteLogo(multipartFile, settingBag);
        saveCurrencySymbol(request, settingBag);
        updateSettingValuesFromForm(request, settingBag.list());
        redirectAttributes.addFlashAttribute("message", "General settings have been saved");
        return "redirect:/settings";
    }

    @PostMapping("/save-mail-server")
    public String saveMailServerSettings(HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        List<Setting> mailServerSettings = settingService.getMailServerSettings();
        updateSettingValuesFromForm(request, mailServerSettings);
        redirectAttributes.addFlashAttribute("message", "Mail Server settings have been saved");
        return "redirect:/settings";
    }

    @PostMapping("/save-mail-templates")
    public String saveMailServerTemplates(HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        List<Setting> mailServerTemplatesSettings = settingService.getMailServerTemplatesSettings();
        updateSettingValuesFromForm(request, mailServerTemplatesSettings);
        redirectAttributes.addFlashAttribute("message", "Mail templates settings have been saved");
        return "redirect:/settings";
    }

    @PostMapping("/save-payment")
    public String savePaymentSettings(HttpServletRequest request, RedirectAttributes redirectAttributes) throws IOException {
        List<Setting> paymentSettings = settingService.getPaymentSettings();
        updateSettingValuesFromForm(request, paymentSettings);
        redirectAttributes.addFlashAttribute("message", "payment settings have been saved");
        return "redirect:/settings";
    }

    private void updateSettingValuesFromForm(HttpServletRequest request, List<Setting> settingList) {
        for (Setting setting : settingList) {
            String value = request.getParameter(setting.getKey());
            if (value != null) {
                setting.setValue(value);
            }
        }

        settingService.saveAll(settingList);
    }

    private void saveCurrencySymbol(HttpServletRequest request, GeneralSettingBag generalSettingBag) {
        Integer currencyId = Integer.parseInt(request.getParameter("CURRENCY_ID"));
        Optional<Currency> findByIdResult = currencyRepository.findById(currencyId);

        if (findByIdResult.isPresent()) {
            Currency currency = findByIdResult.get();
            String symbol = currency.getSymbol();
            generalSettingBag.updateCurrencySymbol(symbol);
        }
    }

    private void saveSiteLogo(MultipartFile multipartFile, GeneralSettingBag settingBag) throws IOException {
        if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
            String value = "/site_logo/" + fileName;
            settingBag.updateSiteLogo(value);
            String uploadDir = "site_logo";
            AmazonS3Util.removeFolder(uploadDir);
            AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
        }
    }
}
