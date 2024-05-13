package org.chaz.backoffice.settings.repository;

import org.membershipcommons.entity.settings.Country;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CountryRepository extends CrudRepository<Country, Integer> {
    public List<Country> findAllByOrderByNameDesc();

    public Country findCountryByName(String name);
}
