package org.paykesho.backoffice.user.repository;

import org.commons.entity.User;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends SearchRepository<User, Integer> {
    @Query("select u from User u where u.email = :email")
    public User getUserByEmail(@Param("email") String email);

    public Long countById(Integer id);

    @Query("select u from User u where concat(u.email, ' ', u.firstName, ' ', u.lastName ) like %?1%")
    public Page<User> findAll(String keyword, Pageable pageable);

    @Query("update User u set u.enabled = ?2 where u.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("UPDATE User u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);
    
    @Query("SELECT u FROM User u WHERE u.resetPasswordToken = ?1")
    public User findByResetPasswordToken(String token);

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================
    
    /**
     * Find user by UUID for public/URL access
     * @param uuid The user's UUID
     * @return Optional containing the user if found
     */
    Optional<User> findByUuid(String uuid);
    
    /**
     * Check if a user exists with the given UUID
     * @param uuid The user's UUID
     * @return true if user exists, false otherwise
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.uuid = ?1")
    boolean existsByUuid(String uuid);
}
