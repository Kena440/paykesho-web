package org.chaz.csoportal.csoadmins.service;


import net.bytebuddy.utility.RandomString;
import org.chaz.csoportal.csoadmins.repository.CSOAdminRepository;
import org.chaz.csoportal.config.Utility;
import org.chaz.csoportal.settings.repository.CountryRepository;
import org.commons.entity.cso.CSOAdmin;
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
public class CSOAdminService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CSOAdminRepository csoAdminRepository;

    @Autowired

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(String email) {
        CSOAdmin csoAdmin = csoAdminRepository.findByEmail(email);
        return csoAdmin == null;
    }

    public void registerCSOAdmin(CSOAdmin csoAdmin) {
        encodePassword(csoAdmin);
        csoAdmin.setEnabled(false);
        csoAdmin.setCreatedTime(new Date());
        csoAdmin.setAuthenticationType(AuthenticationType.DATABASE);

        String randomCode = RandomString.make(64);
        csoAdmin.setVerificationCode(randomCode);
        csoAdminRepository.save(csoAdmin);
    }

    private void encodePassword(CSOAdmin csoAdmin) {
        String encodedPassword = passwordEncoder.encode(csoAdmin.getPassword());
        csoAdmin.setPassword(encodedPassword);
    }

    public boolean verifyCSOAdmin(String verificationCode) {
        CSOAdmin csoAdmin = csoAdminRepository.findByVerificationCode(verificationCode);

        if (csoAdmin == null || csoAdmin.isEnabled()) {
            return false;
        } else {
            csoAdminRepository.enable(csoAdmin.getId());
            return true;
        }
    }

    public void updateAuthenticationType(CSOAdmin csoAdmin, AuthenticationType authenticationType) {
        if (!csoAdmin.getAuthenticationType().equals(authenticationType)) {
            csoAdminRepository.updateAuthenticationType(csoAdmin.getId(), authenticationType);
        }
    }

    public CSOAdmin getCSOAdminByEmail(String email) {
        return csoAdminRepository.findByEmail(email);
    }

    public CSOAdmin getCSOAdminByResetToken(String token) {
        return csoAdminRepository.findByResetToken(token);
    }


    public void addNewCSOAdminUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        CSOAdmin csoAdmin = new CSOAdmin();
        csoAdmin.setEmail(email);
        setName(name, csoAdmin);

        csoAdmin.setName(name);
        csoAdmin.setEnabled(true);
        csoAdmin.setCreatedTime(new Date());
        csoAdmin.setAuthenticationType(authenticationType);
        csoAdmin.setPassword("");

        csoAdminRepository.save(csoAdmin);
    }

    private void setName(String name, CSOAdmin csoAdmin) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            csoAdmin.setName(name);
        } else {
            String firstName = nameArray[0];
            csoAdmin.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            csoAdmin.setLastName(lastName);*/
        }
    }

    public void updateCSOAdmin(CSOAdmin csoAdminInForm) {
        CSOAdmin csoAdminInDB = csoAdminRepository.findById(csoAdminInForm.getId()).get();

        if (csoAdminInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
            if (!csoAdminInForm.getPassword().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(csoAdminInForm.getPassword());
                csoAdminInForm.setPassword(encodedPassword);
            } else {
                csoAdminInForm.setPassword(csoAdminInDB.getPassword());
            }
        } else {
            csoAdminInForm.setPassword(csoAdminInDB.getPassword());
        }

        csoAdminInForm.setTitle(csoAdminInDB.getTitle());
        csoAdminInForm.setEnabled(csoAdminInDB.isEnabled());
        csoAdminInForm.setCreatedTime(csoAdminInDB.getCreatedTime());
        csoAdminInForm.setVerificationCode(csoAdminInDB.getVerificationCode());
        csoAdminInForm.setAuthenticationType(csoAdminInDB.getAuthenticationType());
        csoAdminInForm.setResetPasswordToken(csoAdminInDB.getResetPasswordToken());
        csoAdminRepository.save(csoAdminInForm);
    }

    public String updateCSOAdminResetToken(String email) throws AssessmentToolError {
        CSOAdmin csoAdmin = csoAdminRepository.findByEmail(email);

        if (csoAdmin != null) {
            String token = RandomString.make(30);
            csoAdmin.setResetPasswordToken(token);
            csoAdminRepository.save(csoAdmin);

            return token;
        } else {
            throw new AssessmentToolError("customer not found");
        }
    }

    public void updatePassword(String token, String newPassword) throws AssessmentToolError {
        CSOAdmin csoAdmin = csoAdminRepository.findByResetToken(token);
        if (csoAdmin == null) {
            throw new AssessmentToolError("no customer found. invalid token");
        }

        csoAdmin.setPassword(newPassword);
        csoAdmin.setResetPasswordToken(null);
        encodePassword(csoAdmin);
        csoAdminRepository.save(csoAdmin);
    }

    public CSOAdmin getAuthenticatedCSOAdmin(HttpServletRequest request) {
        String email = Utility.getEmailOfAuthenticatedCSOAdmin(request);
        return getCSOAdminByEmail(email);
    }

    public void increaseFailedAttempts(CSOAdmin csoAdmin) {
        int newFailAttempts = csoAdmin.getFailedAttempt() + 1;
        csoAdminRepository.updateFailedAttempts(newFailAttempts, csoAdmin.getEmail());
    }

    public void resetFailedAttempts(String email) {
        csoAdminRepository.updateFailedAttempts(0, email);
    }

    public void lock(CSOAdmin csoAdmin) {
        csoAdmin.setAccountNonLocked(false);
        csoAdmin.setLockTime(new Date());

        csoAdminRepository.save(csoAdmin);
    }

    public boolean unlockWhenTimeExpired(CSOAdmin csoAdmin) {
        long lockTimeInMillis = csoAdmin.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            csoAdmin.setAccountNonLocked(true);
            csoAdmin.setLockTime(null);
            csoAdmin.setFailedAttempt(0);

            csoAdminRepository.save(csoAdmin);

            return true;
        }

        return false;
    }
}
