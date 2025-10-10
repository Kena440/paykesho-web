package org.paykesho.merchantportal.merchantadmins.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.config.Utility;
import org.paykesho.merchantportal.merchantadmins.repository.MerchantAdminRepository;
import org.paykesho.merchantportal.settings.repository.CountryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantAdminService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private final PasswordEncoder passwordEncoder;
    private final CountryRepository countryRepository;
    private final MerchantAdminRepository merchantAdminRepository;

    public List<Country> countryList() {
        try {
            log.debug("Retrieving country list");
            List<Country> countries = countryRepository.findAllByOrderByNameDesc();
            log.debug("Successfully retrieved {} countries", countries.size());
            return countries;
        } catch (Exception ex) {
            log.error("Error retrieving country list: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve country list", ex);
        }
    }

    public boolean isEmailUnique(String email) {
        try {
            log.debug("Checking email uniqueness for: {}", email);
            MerchantAdmin merchantAdmin = merchantAdminRepository.findByEmail(email);
            boolean isUnique = merchantAdmin == null;
            log.debug("Email {} is unique: {}", email, isUnique);
            return isUnique;
        } catch (Exception ex) {
            log.error("Error checking email uniqueness for {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to check email uniqueness", ex);
        }
    }

    public void registerMerchantAdmin(MerchantAdmin merchantAdmin) {
        try {
            log.debug("Registering new merchant admin with email: {}", merchantAdmin.getEmail());
            encodePassword(merchantAdmin);
            merchantAdmin.setEnabled(false);
            merchantAdmin.setCreatedTime(new Date());
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);

            String randomCode = RandomString.make(64);
            merchantAdmin.setVerificationCode(randomCode);
            merchantAdminRepository.save(merchantAdmin);
            log.debug("Successfully registered merchant admin with ID: {}", merchantAdmin.getId());
        } catch (Exception ex) {
            log.error("Error registering merchant admin with email {}: {}", merchantAdmin.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to register merchant admin", ex);
        }
    }

    private void encodePassword(MerchantAdmin merchantAdmin) {
        String encodedPassword = passwordEncoder.encode(merchantAdmin.getPassword());
        merchantAdmin.setPassword(encodedPassword);
    }

    public boolean verifyMerchantAdmin(String verificationCode) {
        MerchantAdmin merchantAdmin = merchantAdminRepository.findByVerificationCode(verificationCode);

        if (merchantAdmin == null || merchantAdmin.isEnabled()) {
            return false;
        } else {
            merchantAdminRepository.enable(merchantAdmin.getId());
            return true;
        }
    }

    public void updateAuthenticationType(MerchantAdmin merchantAdmin, AuthenticationType authenticationType) {
        if (!merchantAdmin.getAuthenticationType().equals(authenticationType)) {
            merchantAdminRepository.updateAuthenticationType(merchantAdmin.getId(), authenticationType);
        }
    }

    public MerchantAdmin getMerchantAdminByEmail(String email) {
        try {
            log.debug("Retrieving merchant admin by email: {}", email);
            MerchantAdmin merchantAdmin = merchantAdminRepository.findByEmail(email);
            if (merchantAdmin != null) {
                log.debug("Found merchant admin with ID: {}", merchantAdmin.getId());
            } else {
                log.debug("No merchant admin found with email: {}", email);
            }
            return merchantAdmin;
        } catch (Exception ex) {
            log.error("Error retrieving merchant admin by email {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve merchant admin by email", ex);
        }
    }

    public MerchantAdmin getMerchantAdminByResetToken(String token) {
        return merchantAdminRepository.findByResetToken(token);
    }


    public void addNewMerchantAdminUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        MerchantAdmin merchantAdmin = new MerchantAdmin();
        merchantAdmin.setEmail(email);
        setName(name, merchantAdmin);

        merchantAdmin.setName(name);
        merchantAdmin.setEnabled(true);
        merchantAdmin.setCreatedTime(new Date());
        merchantAdmin.setAuthenticationType(authenticationType);
        merchantAdmin.setPassword("");

        merchantAdminRepository.save(merchantAdmin);
    }

    private void setName(String name, MerchantAdmin merchantAdmin) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            merchantAdmin.setName(name);
        } else {
            String firstName = nameArray[0];
            merchantAdmin.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            csoAdmin.setLastName(lastName);*/
        }
    }

    public void updateMerchantAdmin(MerchantAdmin merchantAdminInForm) {
        MerchantAdmin merchantAdminInDB = merchantAdminRepository.findById(merchantAdminInForm.getId()).get();

        if (merchantAdminInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
            if (!merchantAdminInForm.getPassword().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(merchantAdminInForm.getPassword());
                merchantAdminInForm.setPassword(encodedPassword);
            } else {
                merchantAdminInForm.setPassword(merchantAdminInDB.getPassword());
            }
        } else {
            merchantAdminInForm.setPassword(merchantAdminInDB.getPassword());
        }

        merchantAdminInForm.setTitle(merchantAdminInDB.getTitle());
        merchantAdminInForm.setEnabled(merchantAdminInDB.isEnabled());
        merchantAdminInForm.setCreatedTime(merchantAdminInDB.getCreatedTime());
        merchantAdminInForm.setVerificationCode(merchantAdminInDB.getVerificationCode());
        merchantAdminInForm.setAuthenticationType(merchantAdminInDB.getAuthenticationType());
        merchantAdminInForm.setResetPasswordToken(merchantAdminInDB.getResetPasswordToken());
        merchantAdminRepository.save(merchantAdminInForm);
    }

    public String updateMerchantAdminResetToken(String email) throws PaykeshoError {
        MerchantAdmin merchantAdmin = merchantAdminRepository.findByEmail(email);

        if (merchantAdmin != null) {
            String token = RandomString.make(30);
            merchantAdmin.setResetPasswordToken(token);
            merchantAdminRepository.save(merchantAdmin);

            return token;
        } else {
            throw new PaykeshoError("Merchant admin not found with this email address");
        }
    }

    public void updatePassword(String token, String newPassword) throws PaykeshoError {
        MerchantAdmin merchantAdmin = merchantAdminRepository.findByResetToken(token);
        if (merchantAdmin == null) {
            throw new PaykeshoError("Invalid reset token. Merchant admin not found");
        }

        merchantAdmin.setPassword(newPassword);
        merchantAdmin.setResetPasswordToken(null);
        merchantAdmin.setEnabled(true); // Enable account after successful password reset
        encodePassword(merchantAdmin);
        merchantAdminRepository.save(merchantAdmin);
    }

    public MerchantAdmin getAuthenticatedMerchantAdmin(HttpServletRequest request) {
        String email = Utility.getEKeshofAuthenticatedMerchantAdmin(request);
        return getMerchantAdminByEmail(email);
    }

    public void increaseFailedAttempts(MerchantAdmin merchantAdmin) {
        int newFailAttempts = merchantAdmin.getFailedAttempt() + 1;
        merchantAdminRepository.updateFailedAttempts(newFailAttempts, merchantAdmin.getEmail());
    }

    public void resetFailedAttempts(String email) {
        merchantAdminRepository.updateFailedAttempts(0, email);
    }

    public void lock(MerchantAdmin merchantAdmin) {
        merchantAdmin.setAccountNonLocked(false);
        merchantAdmin.setLockTime(new Date());

        merchantAdminRepository.save(merchantAdmin);
    }

    public boolean unlockWhenTimeExpired(MerchantAdmin merchantAdmin) {
        long lockTimeInMillis = merchantAdmin.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            merchantAdmin.setAccountNonLocked(true);
            merchantAdmin.setLockTime(null);
            merchantAdmin.setFailedAttempt(0);

            merchantAdminRepository.save(merchantAdmin);

            return true;
        }

        return false;
    }
}
