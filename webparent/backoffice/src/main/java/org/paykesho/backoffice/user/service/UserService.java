package org.paykesho.backoffice.user.service;

import org.commons.entity.Role;
import org.commons.entity.User;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.user.repository.RoleRepository;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class UserService {

    public static final int USERS_PER_PAGE = 10;
    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<User> listAll() {
        return (List<User>) userRepository.findAll(Sort.by("firstName").ascending());
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
            return userRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new PaykeshoError("Could not find any user");
        }
    }

    /**
     * +
     * This method deletes a user based on ID produced
     *
     * @param id
     * @throws PaykeshoError
     */
    @Transactional(rollbackOn = {Exception.class})
    public void delete(Integer id) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("Null parameter id");
        }

        Long countById = userRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new PaykeshoError("Could not find any user");
        }

        User user = getUserById(id);
        if (user.getId() == null) throw new PaykeshoError("Could not find any driver");
        user.setDeleted(true);
        save(user);
        // userRepository.deleteById(id);
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

}
