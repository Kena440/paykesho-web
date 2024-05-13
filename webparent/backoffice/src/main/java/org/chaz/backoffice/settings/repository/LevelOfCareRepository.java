package org.chaz.backoffice.settings.repository;

import org.membershipcommons.entity.members.LevelOfCare;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LevelOfCareRepository extends CrudRepository<LevelOfCare, Integer> {
    public List<LevelOfCare> findAllByOrderByNameAsc();
}
