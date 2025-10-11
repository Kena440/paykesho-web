package org.paykesho.backoffice.merchants.controller;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.State;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Setting;
import org.commons.enums.FileType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.MerchantCodeService;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/merchants")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MerchantCodeService merchantCodeService;
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("")
    public String listAllMerchantsByPage(Model model) throws PaykeshoError {
        model.addAttribute("pageTitle", "List Merchants");
        model.addAttribute("merchantList", merchantService.listAll());
        return "merchants/merchants";
    }

    @GetMapping("/new")
    public String newMerchant(Model model) {
        String pageTitle = "New Merchant";
        Merchant merchant = new Merchant();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        merchant.setLongitude(28.3228F);
        merchant.setLatitude((float) -15.3875);
        merchant.setEnabled(true);

        model.addAttribute("merchant", merchant);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "merchants/merchant_form";
    }

    @GetMapping("/edit/{uuid}")
    public String editMerchant(@PathVariable(name = "uuid") String uuid, RedirectAttributes redirectAttributes, Model model) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "Edit Merchant";

        try {
            Merchant merchant = merchantService.getMerchantByUuid(uuid);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("merchant", merchant);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "merchants/merchant_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }

    }

    @PostMapping("/save")
    public String saveMerchant(
            @ModelAttribute Merchant merchant,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "image", required = false) MultipartFile multipartFile
    ) {
        log.info("Saving merchant with ID: {}, Name: {}", merchant.getId(), merchant.getName());
        try {
            if (merchant.getId() == null) { // New merchant case
                String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_MNT");
                if (paykeshoRef == null) {
                    redirectAttributes.addFlashAttribute("error", "Failed to generate merchant code. Please try again.");
                    return "redirect:/merchants/new";
                }

                merchant.setCode(paykeshoRef);
                merchant.setRegistrationDate(new Date());
            } else { // Editing an existing merchant
                Merchant existingMerchant;
                try {
                    existingMerchant = merchantService.getMerchantById(merchant.getId());
                    if (existingMerchant == null) {
                        redirectAttributes.addFlashAttribute("error", "Merchant not found. Please refresh and try again.");
                        return "redirect:/merchants";
                    }
                } catch (PaykeshoError e) {
                    redirectAttributes.addFlashAttribute("error", "Merchant not found: " + e.getMessage());
                    return "redirect:/merchants";
                }

                merchant.setCode(existingMerchant.getCode()); // Preserve the original code
                merchant.setRegistrationDate(existingMerchant.getRegistrationDate()); // Preserve registration date

                // If no new image is uploaded, keep the old image
                if (multipartFile == null || multipartFile.isEmpty()) {
                    merchant.setRemoteFileId(existingMerchant.getRemoteFileId());
                    merchant.setPhotos(existingMerchant.getPhotos());
                    merchant.setPhotoPath(existingMerchant.getPhotoPath());
                }
            }

            // Handle new image upload if provided
            if (multipartFile != null && !multipartFile.isEmpty()) {
                FileUploadResponse uploadResponse = fileUploadService.uploadFile(
                        multipartFile, FileType.logo, 1, "logo photo for " + merchant.getEmail());

                if (!uploadResponse.isStatus()) {
                    String errorMessage = uploadResponse.getMessage() != null ? 
                        "File upload failed: " + uploadResponse.getMessage() : 
                        "File upload failed. Please try again.";
                    redirectAttributes.addFlashAttribute("error", errorMessage);
                    
                    if (merchant.getId() != null) {
                        return "redirect:/merchants/edit/" + merchant.getUuid();
                    } else {
                        return "redirect:/merchants/new";
                    }
                }

                merchant.setRemoteFileId(uploadResponse.getFileId());
                merchant.setPhotos(uploadResponse.getFilePath());
                merchant.setPhotoPath(uploadResponse.getFilePath());
            }

            // Save the merchant
            merchantService.save(merchant);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                    "Merchant saved successfully: " + merchant.getEmail());

            return "redirect:/merchants";
            
        } catch (Exception e) {
            // Handle all errors gracefully
            log.error("Error saving merchant: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error saving merchant: " + e.getMessage());
            
            if (merchant.getId() != null) {
                return "redirect:/merchants/edit/" + merchant.getId();
            } else {
                return "redirect:/merchants/new";
            }
        }
    }


    @GetMapping("/view/{uuid}")
    public String viewMerchant(@PathVariable(name = "uuid") String uuid, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "View Merchant";

        try {

            Merchant merchant = merchantService.getMerchantByUuid(uuid);
            MerchantAdmin merchantAdmin = merchant.getContactPerson();
            if (merchantAdmin == null) merchantAdmin = new MerchantAdmin();

            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(merchant.getRemoteFileId());
            if (fileAccessResponse != null) {
                merchant.setPhotoPath(fileAccessResponse.getFileUrl());
            }

            loadCurrencySetting(request);
            model.addAttribute("qrCodeContent", "/merchants/generateQRCode?qrContent=" + merchant.getCode());
            model.addAttribute("merchant", merchant);
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("merchantAdmin", merchantAdmin);

            return "merchants/merchant_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }
    }


    @GetMapping("/{uuid}/enabled/{status}")
    public String updateMerchantEnabledStatus(@PathVariable(name = "uuid") String uuid, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            Merchant merchant = merchantService.getMerchantByUuid(uuid);
            merchantService.updateMerchantStatusByUuid(uuid, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = merchant.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_MERCHANTS;

    }

    @GetMapping("/details/{uuid}")
    public String viewMerchantDetails(@PathVariable("uuid") String uuid, Model model) throws PaykeshoError {

        Merchant merchant = merchantService.getMerchantByUuid(uuid);
        model.addAttribute("merchant", merchant);
        return "merchants/merchant_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportMerchantsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError {
        switch (format) {
            case "csv":
                //(response);
                break;
            case "excel":
                //exportToExcel(response);
                break;
            case "pdf":
                //exportToPDF(response);
                break;
            default:
                throw new PaykeshoError("no format provided");
        }
    }

    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        byte[] qrCode = merchantCodeService.generateQRCode(qrContent, 500, 500);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(qrCode);
    }
    
    @GetMapping("/debug")
    public String debugPage(Model model) {
        Merchant merchant = new Merchant();
        merchant.setName("Debug Merchant");
        merchant.setEmail("debug@test.com");
        merchant.setPhoneNumber("123456789");
        merchant.setMerchantType(org.commons.enums.MerchantType.CAFE);
        merchant.setDetails("Debug details");
        merchant.setServicesOffered("Debug services");
        merchant.setEnabled(true);
        
        model.addAttribute("merchant", merchant);
        model.addAttribute("pageTitle", "Debug Merchant");
        model.addAttribute("message", "Debug page working correctly");
        return "merchants/merchant_form";
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
