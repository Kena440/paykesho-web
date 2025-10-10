package org.paykesho.organizationportal.organizationadmins.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.config.Utility;
import org.paykesho.organizationportal.organizationadmins.repository.OrganizationAdminRepository;
import org.paykesho.organizationportal.settings.repository.CountryRepository;
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
public class OrganizationAdminService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private final CountryRepository countryRepository;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final PasswordEncoder passwordEncoder;

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
            OrganizationAdmin organizationAdmin = organizationAdminRepository.findByEmail(email);
            boolean isUnique = organizationAdmin == null;
            log.debug("Email {} is unique: {}", email, isUnique);
            return isUnique;
        } catch (Exception ex) {
            log.error("Error checking email uniqueness for {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to check email uniqueness", ex);
        }
    }

    public void registerOrganizationAdmin(OrganizationAdmin organizationAdmin) {
        try {
            log.debug("Registering new organization admin with email: {}", organizationAdmin.getEmail());
            encodePassword(organizationAdmin);
            organizationAdmin.setEnabled(false);
            organizationAdmin.setCreatedTime(new Date());
            organizationAdmin.setAuthenticationType(AuthenticationType.DATABASE);

            String randomCode = RandomString.make(64);
            organizationAdmin.setVerificationCode(randomCode);
            organizationAdminRepository.save(organizationAdmin);
            log.debug("Successfully registered organization admin with ID: {}", organizationAdmin.getId());
        } catch (Exception ex) {
            log.error("Error registering organization admin with email {}: {}", organizationAdmin.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to register organization admin", ex);
        }
    }

    private void encodePassword(OrganizationAdmin organizationAdmin) {
        String encodedPassword = passwordEncoder.encode(organizationAdmin.getPassword());
        organizationAdmin.setPassword(encodedPassword);
    }

    public boolean verifyOrganizationAdmin(String verificationCode) {
        OrganizationAdmin organizationAdmin = organizationAdminRepository.findByVerificationCode(verificationCode);

        if (organizationAdmin == null || organizationAdmin.isEnabled()) {
            return false;
        } else {
            organizationAdminRepository.enable(organizationAdmin.getId());
            return true;
        }
    }

    public void updateAuthenticationType(OrganizationAdmin organizationAdmin, AuthenticationType authenticationType) {
        if (!organizationAdmin.getAuthenticationType().equals(authenticationType)) {
            organizationAdminRepository.updateAuthenticationType(organizationAdmin.getId(), authenticationType);
        }
    }

    public OrganizationAdmin getOrganizationAdminByEmail(String email) {
        try {
            log.debug("Retrieving organization admin by email: {}", email);
            OrganizationAdmin organizationAdmin = organizationAdminRepository.findByEmail(email);
            if (organizationAdmin != null) {
                log.debug("Found organization admin with ID: {}", organizationAdmin.getId());
            } else {
                log.debug("No organization admin found with email: {}", email);
            }
            return organizationAdmin;
        } catch (Exception ex) {
            log.error("Error retrieving organization admin by email {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve organization admin by email", ex);
        }
    }

    public OrganizationAdmin getOrganizationAdminByResetToken(String token) {
        return organizationAdminRepository.findByResetToken(token);
    }


    public void addNewOrganizationAdminUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        OrganizationAdmin organizationAdmin = new OrganizationAdmin();
        organizationAdmin.setEmail(email);
        setName(name, organizationAdmin);

        organizationAdmin.setName(name);
        organizationAdmin.setEnabled(true);
        organizationAdmin.setCreatedTime(new Date());
        organizationAdmin.setAuthenticationType(authenticationType);
        organizationAdmin.setPassword("");

        organizationAdminRepository.save(organizationAdmin);
    }

    private void setName(String name, OrganizationAdmin organizationAdmin) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            organizationAdmin.setName(name);
        } else {
            String firstName = nameArray[0];
            organizationAdmin.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            csoAdmin.setLastName(lastName);*/
        }
    }

    public void updateOrganizationAdmin(OrganizationAdmin organizationAdminInForm) {
        OrganizationAdmin organizationAdminInDB = organizationAdminRepository.findById(organizationAdminInForm.getId()).get();

        if (organizationAdminInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
            if (!organizationAdminInForm.getPassword().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(organizationAdminInForm.getPassword());
                organizationAdminInForm.setPassword(encodedPassword);
            } else {
                organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
            }
        } else {
            organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
        }

        organizationAdminInForm.setTitle(organizationAdminInDB.getTitle());
        organizationAdminInForm.setEnabled(organizationAdminInDB.isEnabled());
        organizationAdminInForm.setCreatedTime(organizationAdminInDB.getCreatedTime());
        organizationAdminInForm.setVerificationCode(organizationAdminInDB.getVerificationCode());
        organizationAdminInForm.setAuthenticationType(organizationAdminInDB.getAuthenticationType());
        organizationAdminInForm.setResetPasswordToken(organizationAdminInDB.getResetPasswordToken());
        organizationAdminRepository.save(organizationAdminInForm);
    }

    public String updateOrganizationAdminResetToken(String email) throws PaykeshoError {
        OrganizationAdmin organizationAdmin = organizationAdminRepository.findByEmail(email);

        if (organizationAdmin != null) {
            String token = RandomString.make(30);
            organizationAdmin.setResetPasswordToken(token);
            organizationAdminRepository.save(organizationAdmin);

            return token;
        } else {
            throw new PaykeshoError("Organization admin not found with this email address");
        }
    }

    public void updatePassword(String token, String newPassword) throws PaykeshoError {
        OrganizationAdmin organizationAdmin = organizationAdminRepository.findByResetToken(token);
        if (organizationAdmin == null) {
            throw new PaykeshoError("Invalid reset token. Organization admin not found");
        }

        organizationAdmin.setPassword(newPassword);
        organizationAdmin.setResetPasswordToken(null);
        organizationAdmin.setEnabled(true); // Enable account after successful password reset
        encodePassword(organizationAdmin);
        organizationAdminRepository.save(organizationAdmin);
    }

    public OrganizationAdmin getAuthenticatedOrganizationAdmin(HttpServletRequest request) {
        String email = Utility.getEKeshofAuthenticatedOrganizationAdmin(request);
        return getOrganizationAdminByEmail(email);
    }

    public void increaseFailedAttempts(OrganizationAdmin organizationAdmin) {
        int newFailAttempts = organizationAdmin.getFailedAttempt() + 1;
        organizationAdminRepository.updateFailedAttempts(newFailAttempts, organizationAdmin.getEmail());
    }

    public void resetFailedAttempts(String email) {
        organizationAdminRepository.updateFailedAttempts(0, email);
    }

    public void lock(OrganizationAdmin organizationAdmin) {
        organizationAdmin.setAccountNonLocked(false);
        organizationAdmin.setLockTime(new Date());

        organizationAdminRepository.save(organizationAdmin);
    }

    public boolean unlockWhenTimeExpired(OrganizationAdmin organizationAdmin) {
        long lockTimeInMillis = organizationAdmin.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            organizationAdmin.setAccountNonLocked(true);
            organizationAdmin.setLockTime(null);
            organizationAdmin.setFailedAttempt(0);

            organizationAdminRepository.save(organizationAdmin);

            return true;
        }

        return false;
    }
}
