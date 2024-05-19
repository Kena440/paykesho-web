package org.chaz.portal.settings.repository;

import org.commons.entity.facilities.LevelOfCare;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LevelOfCareRepository extends CrudRepository<LevelOfCare, Integer> {
    public List<LevelOfCare> findAllByOrderByNameAsc();
}
