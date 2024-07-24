package org.chaz.csoportal.csoadmins.repository;

import org.commons.entity.cso.CSOAdmin;
import org.commons.enums.AuthenticationType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CSOAdminRepository extends CrudRepository<CSOAdmin, Integer> {

    @Query("select c from CSOAdmin c where c.email = ?1")
    public CSOAdmin findByEmail(String email);

    @Query("select c from CSOAdmin c where c.verificationCode = ?1")
    public CSOAdmin findByVerificationCode(String code);

    @Query("update CSOAdmin c set c.enabled = true, c.verificationCode = null where c.id = ?1")
    @Modifying
    public void enable(Integer id);

    @Query("update CSOAdmin c set c.authenticationType = ?2 where c.id = ?1")
    @Modifying
    public void updateAuthenticationType(Integer memberId, AuthenticationType authenticationType);

    @Query("select c from CSOAdmin c where c.resetPasswordToken = ?1")
    public CSOAdmin findByResetToken(String token);

    @Query("UPDATE CSOAdmin u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);

}
