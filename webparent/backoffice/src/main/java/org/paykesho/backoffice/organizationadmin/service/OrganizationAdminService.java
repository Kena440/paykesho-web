package org.paykesho.backoffice.organizationadmin.service;

import net.bytebuddy.utility.RandomString;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.config.Utility;
import org.paykesho.backoffice.organizationadmin.repository.OrganizationAdminRepository;
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
public class OrganizationAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationAdminService.class);
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private OrganizationAdminRepository organizationAdminRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private SettingService settingService;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBER_ADMINS_PER_PAGE, organizationAdminRepository);
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        organizationAdminRepository.updateEnabledStatus(id, enabled);
    }

    public OrganizationAdmin get(Integer id) throws PaykeshoError {
        try {
            return organizationAdminRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(Integer id, String email) {
        OrganizationAdmin existingOrganizationAdmin = organizationAdminRepository.findByEmail(email);

        if (existingOrganizationAdmin != null && existingOrganizationAdmin.getId() != id) {
            return false;
        }

        return true;
    }

    public OrganizationAdmin save(HttpServletRequest request, OrganizationAdmin organizationAdminInForm) throws MessagingException, UnsupportedEncodingException {
        OrganizationAdmin organizationAdminInDB = organizationAdminRepository.findByEmail(organizationAdminInForm.getEmail());

        if (organizationAdminInDB == null) {

            String token = RandomString.make(30);
            //String passwordCode = RandomString.make(10);
            String passwordCode = "paykesho";
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(passwordCode);
            organizationAdminInForm.setPassword(encodedPassword);

            organizationAdminInForm.setVerificationCode(verificationCode);
            organizationAdminInForm.setCreatedTime(new Date());
            organizationAdminInForm.setEnabled(true);
            organizationAdminInForm.setAuthenticationType(AuthenticationType.DATABASE);
            organizationAdminInForm.setResetPasswordToken(token);

            new Thread(() -> {
                try {
                    //sendAdminCreationEmail(request, organizationAdminInForm, passwordCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

        } else {
            organizationAdminInForm.setResetPasswordToken(organizationAdminInDB.getResetPasswordToken());
            organizationAdminInForm.setAuthenticationType(organizationAdminInDB.getAuthenticationType());
            organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
            organizationAdminInForm.setVerificationCode(organizationAdminInDB.getVerificationCode());
        }

        return organizationAdminRepository.save(organizationAdminInForm);
    }

    public void delete(Integer id) throws PaykeshoError {
        Long count = organizationAdminRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("could not find customer");
        }
        organizationAdminRepository.deleteById(id);
    }

    public Long countAdmins() {
        return organizationAdminRepository.count();
    }

    private void sendAdminCreationEmail(HttpServletRequest request, OrganizationAdmin organizationAdmin, String passwordCode) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBag = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBag);

        mailSender.setDefaultEncoding("utf-8");

        String toAddress = organizationAdmin.getEmail();
        String subject = emailSettingBag.getOrganizationAdminCreationSubject();
        String content = emailSettingBag.getOrganizationAdminCreationContent();
        String systemURL = Utility.getSiteUrl(request);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBag.getFromAddress(), emailSettingBag.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", organizationAdmin.getFullName());
        content = content.replace("[[link]]", "https://organization.paykesho.com/");
        content = content.replace("[[email]]", organizationAdmin.getEmail());
        content = content.replace("[[password]]", passwordCode);

        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }
}
