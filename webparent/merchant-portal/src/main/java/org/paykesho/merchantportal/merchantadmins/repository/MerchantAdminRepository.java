package org.paykesho.merchantportal.merchantadmins.repository;

import org.commons.entity.merchant.MerchantAdmin;
import org.commons.enums.AuthenticationType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MerchantAdminRepository extends CrudRepository<MerchantAdmin, Integer> {

    @Query("select c from MerchantAdmin c where c.email = ?1")
    public MerchantAdmin findByEmail(String email);

    @Query("select c from MerchantAdmin c where c.verificationCode = ?1")
    public MerchantAdmin findByVerificationCode(String code);

    @Query("update MerchantAdmin c set c.enabled = true, c.verificationCode = null where c.id = ?1")
    @Modifying
    public void enable(Integer id);

    @Query("update MerchantAdmin c set c.authenticationType = ?2 where c.id = ?1")
    @Modifying
    public void updateAuthenticationType(Integer memberId, AuthenticationType authenticationType);

    @Query("select c from MerchantAdmin c where c.resetPasswordToken = ?1")
    public MerchantAdmin findByResetToken(String token);

    @Query("UPDATE MerchantAdmin u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);

}
