package org.paykesho.backoffice.user.repository;

import org.commons.entity.User;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
