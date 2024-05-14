package org.chaz.portal.settings.repository;

import org.commons.entity.settings.FundingProject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FundingProjectRepository extends CrudRepository<FundingProject, Integer> {
    public List<FundingProject> findAllByOrderByProjectName();

    @Query("select f from FundingProject f where f.projectCode = ?1")
    public FundingProject findByCode(String code);
}
