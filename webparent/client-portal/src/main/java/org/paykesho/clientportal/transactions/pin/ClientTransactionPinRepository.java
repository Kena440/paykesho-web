package org.paykesho.clientportal.transactions.pin;

import org.commons.entity.client.ClientTransactionPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface ClientTransactionPinRepository extends JpaRepository<ClientTransactionPin, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ClientTransactionPin p where p.clientId = :clientId")
    Optional<ClientTransactionPin> findByClientIdForUpdate(@Param("clientId") Integer clientId);
}
