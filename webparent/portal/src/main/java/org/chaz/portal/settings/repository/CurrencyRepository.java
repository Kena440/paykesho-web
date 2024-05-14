package org.chaz.portal.settings.repository;

import org.commons.entity.settings.Currency;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CurrencyRepository extends CrudRepository<Currency, Integer> {
    public List<Currency> findAllByOrderByNameDesc();
}
