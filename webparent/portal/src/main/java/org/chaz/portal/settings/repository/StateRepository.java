package org.chaz.membershipsite.settings.repository;

import org.membershipcommons.entity.State;
import org.membershipcommons.entity.settings.Country;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StateRepository extends CrudRepository<State, Integer> {
    public List<State> findByCountryOrderByNameAsc(Country country);
}
