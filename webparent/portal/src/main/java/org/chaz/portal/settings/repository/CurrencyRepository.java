package org.chaz.membershipsite.settings.repository;

import org.membershipcommons.entity.settings.Currency;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CurrencyRepository extends CrudRepository<Currency, Integer> {
    public List<Currency> findAllByOrderByNameDesc();
}
