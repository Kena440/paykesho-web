package org.paykesho.merchantportal.settings.repository;

import org.commons.entity.State;
import org.commons.entity.settings.Country;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StateRepository extends CrudRepository<State, Integer> {
    public List<State> findByCountryOrderByNameAsc(Country country);

    @Query("SELECT s FROM State s WHERE s.country.uuid = ?1 ORDER BY s.name ASC")
    List<State> findByCountryUuid(String countryUuid);
}
