package org.chaz.portal.assessors.repository;

import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.enums.AuthenticationType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MemberAdminRepository extends CrudRepository<MemberAdmin, Integer> {

    @Query("select c from Assessor c where c.email = ?1")
    public MemberAdmin findByEmail(String email);

    @Query("select c from Assessor c where c.verificationCode = ?1")
    public MemberAdmin findByVerificationCode(String code);

    @Query("update Assessor c set c.enabled = true, c.verificationCode = null where c.id = ?1")
    @Modifying
    public void enable(Integer id);

    @Query("update Assessor c set c.authenticationType = ?2 where c.id = ?1")
    @Modifying
    public void updateAuthenticationType(Integer memberId, AuthenticationType authenticationType);

    @Query("select c from Assessor c where c.resetPasswordToken = ?1")
    public MemberAdmin findByResetToken(String token);

    @Query("UPDATE Assessor u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);

}
