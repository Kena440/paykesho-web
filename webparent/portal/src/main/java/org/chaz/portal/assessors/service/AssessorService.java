package org.chaz.portal.assessors.service;


import net.bytebuddy.utility.RandomString;
import org.chaz.portal.assessors.repository.AssessorRepository;
import org.chaz.portal.config.Utility;
import org.chaz.portal.settings.repository.CountryRepository;
import org.commons.entity.assessment.Assessor;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AssessorService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private AssessorRepository assessorRepository;

    @Autowired

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(String email) {
        Assessor assessor = assessorRepository.findByEmail(email);
        return assessor == null;
    }

    public void registerAssessor(Assessor assessor) {
        encodePassword(assessor);
        assessor.setEnabled(false);
        assessor.setCreatedTime(new Date());
        assessor.setAuthenticationType(AuthenticationType.DATABASE);

        String randomCode = RandomString.make(64);
        assessor.setVerificationCode(randomCode);
        assessorRepository.save(assessor);
    }

    private void encodePassword(Assessor assessor) {
        String encodedPassword = passwordEncoder.encode(assessor.getPassword());
        assessor.setPassword(encodedPassword);
    }

    public boolean verifyAssessor(String verificationCode) {
        Assessor assessor = assessorRepository.findByVerificationCode(verificationCode);

        if (assessor == null || assessor.isEnabled()) {
            return false;
        } else {
            assessorRepository.enable(assessor.getId());
            return true;
        }
    }

    public void updateAuthenticationType(Assessor assessor, AuthenticationType authenticationType) {
        if (!assessor.getAuthenticationType().equals(authenticationType)) {
            assessorRepository.updateAuthenticationType(assessor.getId(), authenticationType);
        }
    }

    public Assessor getAssessorByEmail(String email) {
        return assessorRepository.findByEmail(email);
    }

    public Assessor getAssessorByResetToken(String token) {
        return assessorRepository.findByResetToken(token);
    }


    public void addNewAssessorUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        Assessor assessor = new Assessor();
        assessor.setEmail(email);
        setName(name, assessor);

        assessor.setName(name);
        assessor.setEnabled(true);
        assessor.setCreatedTime(new Date());
        assessor.setAuthenticationType(authenticationType);
        assessor.setPassword("");

        assessorRepository.save(assessor);
    }

    private void setName(String name, Assessor assessor) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            assessor.setName(name);
        } else {
            String firstName = nameArray[0];
            assessor.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            assessor.setLastName(lastName);*/
        }
    }

    public void updateAssessor(Assessor assessorInForm) {
        Assessor assessorInDB = assessorRepository.findById(assessorInForm.getId()).get();

        if (assessorInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
            if (!assessorInForm.getPassword().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(assessorInForm.getPassword());
                assessorInForm.setPassword(encodedPassword);
            } else {
                assessorInForm.setPassword(assessorInDB.getPassword());
            }
        } else {
            assessorInForm.setPassword(assessorInDB.getPassword());
        }

        assessorInForm.setTitle(assessorInDB.getTitle());
        assessorInForm.setEnabled(assessorInDB.isEnabled());
        assessorInForm.setCreatedTime(assessorInDB.getCreatedTime());
        assessorInForm.setVerificationCode(assessorInDB.getVerificationCode());
        assessorInForm.setAuthenticationType(assessorInDB.getAuthenticationType());
        assessorInForm.setResetPasswordToken(assessorInDB.getResetPasswordToken());
        assessorRepository.save(assessorInForm);
    }

    public String updateAssessorResetToken(String email) throws AssessmentToolError {
        Assessor assessor = assessorRepository.findByEmail(email);

        if (assessor != null) {
            String token = RandomString.make(30);
            assessor.setResetPasswordToken(token);
            assessorRepository.save(assessor);

            return token;
        } else {
            throw new AssessmentToolError("customer not found");
        }
    }

    public void updatePassword(String token, String newPassword) throws AssessmentToolError {
        Assessor assessor = assessorRepository.findByResetToken(token);
        if (assessor == null) {
            throw new AssessmentToolError("no customer found. invalid token");
        }

        assessor.setPassword(newPassword);
        assessor.setResetPasswordToken(null);
        encodePassword(assessor);
        assessorRepository.save(assessor);
    }

    public Assessor getAuthenticatedAssessor(HttpServletRequest request) {
        String email = Utility.getEmailOfAuthenticatedAssessor(request);
        return getAssessorByEmail(email);
    }

    public void increaseFailedAttempts(Assessor assessor) {
        int newFailAttempts = assessor.getFailedAttempt() + 1;
        assessorRepository.updateFailedAttempts(newFailAttempts, assessor.getEmail());
    }

    public void resetFailedAttempts(String email) {
        assessorRepository.updateFailedAttempts(0, email);
    }

    public void lock(Assessor assessor) {
        assessor.setAccountNonLocked(false);
        assessor.setLockTime(new Date());

        assessorRepository.save(assessor);
    }

    public boolean unlockWhenTimeExpired(Assessor assessor) {
        long lockTimeInMillis = assessor.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            assessor.setAccountNonLocked(true);
            assessor.setLockTime(null);
            assessor.setFailedAttempt(0);

            assessorRepository.save(assessor);

            return true;
        }

        return false;
    }
}
