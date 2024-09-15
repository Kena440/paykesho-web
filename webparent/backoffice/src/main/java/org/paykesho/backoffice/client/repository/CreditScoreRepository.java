package org.paykesho.backoffice.client.repository;

import org.commons.entity.client.ClientBalance;
import org.commons.entity.client.CreditScore;
import org.springframework.data.repository.CrudRepository;

public interface CreditScoreRepository extends CrudRepository<CreditScore, Integer> {
    public CreditScore getCreditScoreByClientId(Integer integer);

}

