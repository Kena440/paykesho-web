package org.chaz.backoffice.assessors.service;

import net.bytebuddy.utility.RandomString;
import org.chaz.backoffice.assessors.repository.AssessorRepository;
import org.chaz.backoffice.config.Utility;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.repository.CountryRepository;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.assessment.Assessor;
import org.commons.enums.AuthenticationType;
import org.commons.utils.AssessmentToolError;
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
public class AssessorService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(AssessorService.class);
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private AssessorRepository assessorRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private SettingService settingService;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, MEMBER_ADMINS_PER_PAGE, assessorRepository);
    }

    public void updateAssessorEnabledStatus(Integer id, boolean enabled) {
        assessorRepository.updateEnabledStatus(id, enabled);
    }

    public Assessor get(Integer id) throws AssessmentToolError {
        try {
            return assessorRepository.findById(id).get();
        } catch (Exception exception) {
            throw new AssessmentToolError("no customer found " + exception);
        }
    }

    public List<Assessor> assessorList() {
        return assessorRepository.getAssessorsByEnabled();
    }

    public boolean isEmailUnique(Integer id, String email) {
        Assessor existingAssessor = assessorRepository.findByEmail(email);

        if (existingAssessor != null && existingAssessor.getId() != id) {
            return false;
        }

        return true;
    }

    public Assessor save(HttpServletRequest request, Assessor assessorInForm) throws MessagingException, UnsupportedEncodingException {
        Assessor assessorInDB = assessorRepository.findByEmail(assessorInForm.getEmail());

        if (assessorInDB == null) {

            String token = RandomString.make(30);
            String passwordCode = RandomString.make(10);
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(passwordCode);
            assessorInForm.setPassword(encodedPassword);

            assessorInForm.setVerificationCode(verificationCode);
            assessorInForm.setCreatedTime(new Date());
            assessorInForm.setEnabled(true);
            assessorInForm.setAuthenticationType(AuthenticationType.DATABASE);
            assessorInForm.setResetPasswordToken(token);

            new Thread(() -> {
                try {
                    sendAdminCreationEmail(request, assessorInForm, passwordCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

        } else {
            assessorInForm.setResetPasswordToken(assessorInDB.getResetPasswordToken());
            assessorInForm.setAuthenticationType(assessorInDB.getAuthenticationType());
            assessorInForm.setPassword(assessorInDB.getPassword());
            assessorInForm.setVerificationCode(assessorInDB.getVerificationCode());
        }

        return assessorRepository.save(assessorInForm);
    }

    public void delete(Integer id) throws AssessmentToolError {
        Long count = assessorRepository.countById(id);
        if (count == null || count == 0) {
            throw new AssessmentToolError("could not find customer");
        }
        assessorRepository.deleteById(id);
    }

    public Long countAdmins() {
        return assessorRepository.count();
    }

    private void sendAdminCreationEmail(HttpServletRequest request, Assessor assessor, String passwordCode) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBag = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBag);

        mailSender.setDefaultEncoding("utf-8");

        String toAddress = assessor.getEmail();
        String subject = emailSettingBag.getAssessorCreationSubject();
        String content = emailSettingBag.getAssessorCreationContent();
        String systemURL = Utility.getSiteUrl(request);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBag.getFromAddress(), emailSettingBag.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", assessor.getFullName());
        content = content.replace("[[link]]", "https://assessment.chaz.domains/");
        content = content.replace("[[email]]", assessor.getEmail());
        content = content.replace("[[password]]", passwordCode);

        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }
}
