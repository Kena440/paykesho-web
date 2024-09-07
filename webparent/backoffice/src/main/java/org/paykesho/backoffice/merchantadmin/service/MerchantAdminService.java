package org.paykesho.backoffice.merchantadmin.service;

import net.bytebuddy.utility.RandomString;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.config.Utility;
import org.paykesho.backoffice.merchantadmin.repository.MerchantAdminRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.email.EmailSettingBag;
import org.paykesho.backoffice.settings.repository.CountryRepository;
import org.paykesho.backoffice.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class MerchantAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantAdminService.class);
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private MerchantAdminRepository merchantAdminRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private SettingService settingService;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBER_ADMINS_PER_PAGE, merchantAdminRepository);
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        merchantAdminRepository.updateEnabledStatus(id, enabled);
    }

    public MerchantAdmin get(Integer id) throws PaykeshoError {
        try {
            return merchantAdminRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(Integer id, String email) {
        MerchantAdmin existingMerchantAdmin = merchantAdminRepository.findByEmail(email);

        if (existingMerchantAdmin != null && existingMerchantAdmin.getId() != id) {
            return false;
        }

        return true;
    }

    public MerchantAdmin save(HttpServletRequest request, MerchantAdmin merchantAdmin) throws MessagingException, UnsupportedEncodingException {
        MerchantAdmin merchantAdminInDB = merchantAdminRepository.findByEmail(merchantAdmin.getEmail());

        if (merchantAdminInDB == null) {

            String token = RandomString.make(30);
            //String passwordCode = RandomString.make(10);
            String passwordCode = "paykesho";
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(passwordCode);
            merchantAdmin.setPassword(encodedPassword);

            merchantAdmin.setVerificationCode(verificationCode);
            merchantAdmin.setCreatedTime(new Date());
            merchantAdmin.setEnabled(true);
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);
            merchantAdmin.setResetPasswordToken(token);

            new Thread(() -> {
                try {
                    //sendAdminCreationEmail(request, merchantAdmin, passwordCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

        } else {
            merchantAdmin.setResetPasswordToken(merchantAdminInDB.getResetPasswordToken());
            merchantAdmin.setAuthenticationType(merchantAdminInDB.getAuthenticationType());
            merchantAdmin.setPassword(merchantAdminInDB.getPassword());
            merchantAdmin.setVerificationCode(merchantAdminInDB.getVerificationCode());
        }

        return merchantAdminRepository.save(merchantAdmin);
    }

    public void delete(Integer id) throws PaykeshoError {
        Long count = merchantAdminRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("could not find customer");
        }
        merchantAdminRepository.deleteById(id);
    }

    public Long countAdmins() {
        return merchantAdminRepository.count();
    }

    private void sendAdminCreationEmail(HttpServletRequest request, MerchantAdmin merchantAdmin, String passwordCode) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBag = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBag);

        mailSender.setDefaultEncoding("utf-8");

        String toAddress = merchantAdmin.getEmail();
        String subject = emailSettingBag.getMerchantAdminCreationSubject();
        String content = emailSettingBag.getMerchantAdminCreationContent();
        String systemURL = Utility.getSiteUrl(request);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBag.getFromAddress(), emailSettingBag.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", merchantAdmin.getFullName());
        content = content.replace("[[link]]", "https://merchant.paykesho.com/");
        content = content.replace("[[email]]", merchantAdmin.getEmail());
        content = content.replace("[[password]]", passwordCode);

        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }
}
