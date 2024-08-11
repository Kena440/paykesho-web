package org.paykesho.clientportal.clients.repository;

import org.commons.entity.client.Client;
import org.commons.enums.AuthenticationType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ClientRepository extends CrudRepository<Client, Integer> {

    @Query("select c from Client c where c.email = ?1")
    public Client findByEmail(String email);

    @Query("select c from Client c where c.verificationCode = ?1")
    public Client findByVerificationCode(String code);

    @Query("update Client c set c.enabled = true, c.verificationCode = null where c.id = ?1")
    @Modifying
    public void enable(Integer id);

    @Query("update Client c set c.authenticationType = ?2 where c.id = ?1")
    @Modifying
    public void updateAuthenticationType(Integer memberId, AuthenticationType authenticationType);

    @Query("select c from Client c where c.resetPasswordToken = ?1")
    public Client findByResetToken(String token);

    @Query("UPDATE Client u SET u.failedAttempt = ?1 WHERE u.email = ?2")
    @Modifying
    public void updateFailedAttempts(int failAttempts, String email);

    @Query("select count(c.id) as total from Client c")
    public Long countAllClients();

    @Query("select c from Client c where c.code = ?1")
    public Client findByCode(String code);
}
