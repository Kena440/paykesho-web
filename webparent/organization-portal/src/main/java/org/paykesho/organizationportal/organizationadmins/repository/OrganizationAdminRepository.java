package org.paykesho.organizationportal.organizationadmins.repository;

import org.commons.entity.organization.OrganizationAdmin;
import org.commons.enums.AuthenticationType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface OrganizationAdminRepository extends CrudRepository<OrganizationAdmin, Integer> {

    @Query("select c from OrganizationAdmin c where c.email = ?1")
    public OrganizationAdmin findByEmail(String email);

    @Query("select c from OrganizationAdmin c where c.verificationCode = ?1")
    public OrganizationAdmin findByVerificationCode(String code);

    @Query("update OrganizationAdmin c set c.enabled = true, c.verificationCode = null where c.id = ?1")
    @Modifying
    public void enable(Integer id);

    @Query("update OrganizationAdmin c set c.authenticationType = ?2 where c.id = ?1")
    @Modifying
    public void updateAuthenticationType(Integer memberId, AuthenticationType authenticationType);

    @Query("select c from OrganizationAdmin c where c.resetPasswordToken = ?1")
    public OrganizationAdmin findByResetToken(String token);

    @Query("UPDATE OrganizationAdmin u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);

}
