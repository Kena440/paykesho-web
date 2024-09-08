package org.paykesho.clientportal.clients.repository;

import org.commons.entity.client.ClientBalance;
import org.paykesho.clientportal.paging.SearchRepository;
import org.springframework.data.repository.CrudRepository;

public interface ClientBalanceRepository extends CrudRepository<ClientBalance, Integer> {

}

