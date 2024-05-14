package org.chaz.backoffice.settings.repository;

import org.commons.entity.State;
import org.commons.entity.settings.Country;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StateRepository extends CrudRepository<State, Integer> {
    public List<State> findByCountryOrderByNameAsc(Country country);

    public State findStateByName(String name);
}
