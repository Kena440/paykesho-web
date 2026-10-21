package org.paykesho.clientportal.clients.repository;

import org.commons.entity.client.CreditScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditScoreRepository extends CrudRepository<CreditScore, Integer> {

    @Query("select c from CreditScore c where c.client.id = :clientId order by c.lastUpdate desc")
    List<CreditScore> findLatestForClient(@Param("clientId") Integer clientId);
}
