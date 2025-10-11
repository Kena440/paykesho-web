package org.paykesho.backoffice.settings.repository;

import org.commons.entity.settings.Country;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends CrudRepository<Country, Integer> {
    public List<Country> findAllByOrderByNameDesc();

    public Country findCountryByName(String name);

    // UUID-based methods
    Optional<Country> findByUuid(String uuid);
}
