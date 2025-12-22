package org.paykesho.backoffice.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Role;
import org.commons.entity.User;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.user.repository.RoleRepository;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import net.bytebuddy.utility.RandomString;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    public static final int USERS_PER_PAGE = 10;
    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<User> listAll() {
        List<User> users = (List<User>) userRepository.findAll(Sort.by("firstName").ascending());

        for (User user : users) {
            if (user.getPhotoPath() != null && user.getRemoteFileId() != null) {

                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(user.getRemoteFileId());
                if (fileAccessResponse != null) {
                    user.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
        }
        return users;
    }

    /**
     * Get all enabled users (typically admin users) for notification purposes
     *
     * @return List of enabled users
     */
    public List<User> getAllEnabledUsers() {
        List<User> allUsers = listAll();
        return allUsers.stream()
                .filter(user -> user.isEnabled() && !user.isDeleted())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * +
     * This method lists all users in the system by pagination
     *
     * @return
     */
    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, USERS_PER_PAGE, userRepository);

    }

    /**
     * +
     * This method lists all roles in the system
     *
     * @return
     */
    public List<Role> listRoles() {
        return (List<Role>) roleRepository.findAll();
    }

    /**
     * +
     * This method saves user submitted from the user
     *
     * @param user
     */
    @Transactional(rollbackOn = {Exception.class})
    public User save(User user) {
        if (user == null) {
            return null;
        }

        boolean isUpdatingUser = (user.getId() != null);

        if (isUpdatingUser) {
            User existingUser = userRepository.findById(user.getId()).get();
            if (user.getPassword().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            } else {
                encodePassword(user);
            }
        } else {
            encodePassword(user);
        }
        return userRepository.save(user);
    }

    /**
     * +
     * This method encodes the user password
     *
     * @param user
     */
    private void encodePassword(User user) {
        if (user.getPassword() == null) {
            return /*"redirect:/users"*/;
        }
        String encodedPass = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPass);
    }

    /**
     * +
     * This method checks if the email submitted is unique
     *
     * @param id
     * @param email
     * @return
     */
    public boolean isEmailUnique(Integer id, String email) {
        if (email == null) {
            return false;
        }
        User user = userRepository.getUserByEmail(email);

        if (user == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (user != null) return false;
        } else {
            if (user.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public boolean isEmailUniqueByUuid(String uuid, String email) {
        if (email == null) {
            return false;
        }

        // If creating new (no UUID), just check if email exists
        if (uuid == null || uuid.trim().isEmpty()) {
            User existingUser = userRepository.getUserByEmail(email);
            return existingUser == null;
        }

        // If editing, get the current user and exclude it from the check
        try {
            User currentUser = userRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return isEmailUnique(currentUser.getId(), email);
        } catch (Exception e) {
            // If user not found, treat as new
            User existingUser = userRepository.getUserByEmail(email);
            return existingUser == null;
        }
    }

    /**
     * +
     * This method returns a user based on the ID
     *
     * @param id
     * @return
     * @throws PaykeshoError
     */
    public User getUserById(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        try {
            User user = userRepository.findById(id).get();
            
            // Process photoPath the same way as in listAll()
            if (user.getPhotoPath() != null && user.getRemoteFileId() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(user.getRemoteFileId());
                if (fileAccessResponse != null) {
                    user.setPhotoPath(fileAccessResponse.getFileUrl());
                }
            }
            
            return user;
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }
    }


    /**
     * +
     * This method updates the status of a user i.e. enabled or disabled
     *
     * @param id
     * @param enabled
     * @throws PaykeshoError
     */
    public void updateUserStatus(Integer id, boolean enabled) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = userRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any user");
        }

        userRepository.updateEnableStatus(id, enabled);
    }

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================

    /**
     * Get user by UUID (for public/URL access)
     * This method should be used for all public-facing endpoints
     * 
     * @param uuid The user's UUID
     * @return User entity
     * @throws PaykeshoError if UUID is invalid or user not found
     */
    public User getUserByUuid(String uuid) throws PaykeshoError {
        // Validate UUID parameter
        if (uuid == null || uuid.trim().isEmpty()) {
            log.error("getUserByUuid called with null or empty UUID");
            throw new PaykeshoError("Invalid UUID parameter");
        }

        // Validate UUID format to prevent injection attacks
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.error("getUserByUuid called with invalid UUID format: {}", uuid);
            throw new PaykeshoError("Invalid UUID format");
        }

        // Fetch user by UUID
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("User not found with UUID: {}", uuid);
                    return new PaykeshoError("Could not find user with UUID: " + uuid);
                });

        // Ensure UUID is set (in case it was null in database)
        if (user.getUuid() == null || user.getUuid().trim().isEmpty()) {
            log.warn("User {} has null or empty UUID, generating new one", user.getId());
            user.ensureUuid();
            userRepository.save(user);
        }

        // Process photo path
        processPhotoPath(user);

        log.debug("Successfully retrieved user with UUID: {}", uuid);
        return user;
    }

    /**
     * Update user enabled status by UUID
     * 
     * @param uuid The user's UUID
     * @param enabled The new enabled status
     * @throws PaykeshoError if UUID is invalid or user not found
     */
    public void updateUserStatusByUuid(String uuid, boolean enabled) throws PaykeshoError {
        // Get user by UUID (includes validation)
        User user = getUserByUuid(uuid);
        
        // Update status using internal ID
        updateUserStatus(user.getId(), enabled);
        
        log.info("Updated user status for UUID: {} to enabled: {}", uuid, enabled);
    }

    /**
     * Helper method to process user photo path
     * Extracted to avoid code duplication
     * 
     * @param user The user entity
     */
    private void processPhotoPath(User user) {
        if (user != null && user.getPhotoPath() != null && user.getRemoteFileId() != null) {
            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(user.getRemoteFileId());
            if (fileAccessResponse != null) {
                user.setPhotoPath(fileAccessResponse.getFileUrl());
            }
        }
    }

    /**
     * +
     * This method returns a user based on the email
     *
     * @param email
     * @return
     * @throws PaykeshoError
     */
    public User getUserByEmail(String email) throws PaykeshoError {
        if (email == null) {
            throw new PaykeshoError("Null parameter email");
        }

        try {
            return userRepository.getUserByEmail(email);
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }
    }

    public User updateAccount(User userInForm) throws PaykeshoError {
        if (userInForm == null) {
            return null;
        }

        try {
            User userInDB = userRepository.findById(userInForm.getId()).get();
            if (!userInForm.getPassword().isEmpty()) {
                userInDB.setPassword(userInForm.getPassword());
                encodePassword(userInDB);
            }

            if (userInForm.getPhotos() != null) {
                userInDB.setPhotos(userInForm.getPhotos());
            }

            userInDB.setFirstName(userInForm.getFirstName());
            userInDB.setLastName(userInForm.getLastName());

            return userRepository.save(userInDB);
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }

    }

    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempt() + 1;
        userRepository.updateFailedAttempts(newFailAttempts, user.getEmail());
    }

    public void resetFailedAttempts(String email) {
        userRepository.updateFailedAttempts(0, email);
    }

    public void lock(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());

        userRepository.save(user);
    }

    public boolean unlockWhenTimeExpired(User user) {
        long lockTimeInMillis = user.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);

            userRepository.save(user);

            return true;
        }

        return false;
    }
    
    /**
     * Generate and update reset password token for user
     * @param email User email
     * @return Generated token
     * @throws PaykeshoError if user not found
     */
    public String updateUserResetToken(String email) throws PaykeshoError {
        User user = userRepository.getUserByEmail(email);
        
        if (user != null) {
            String token = RandomString.make(30);
            user.setResetPasswordToken(token);
            userRepository.save(user);
            
            log.info("Reset password token generated for user: {}", email);
            return token;
        } else {
            throw new PaykeshoError("User not found");
        }
    }
    
    /**
     * Get user by reset password token
     * @param token Reset password token
     * @return User if found, null otherwise
     */
    public User getUserByResetToken(String token) {
        return userRepository.findByResetPasswordToken(token);
    }
    
    /**
     * Update user password
     * @param user User to update
     * @param newPassword New password (will be encoded)
     */
    public void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);
        userRepository.save(user);
        
        log.info("Password updated for user: {}", user.getEmail());
    }

}
