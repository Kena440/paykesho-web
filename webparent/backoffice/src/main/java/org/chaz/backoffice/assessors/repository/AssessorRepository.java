package org.chaz.backoffice.assessors.repository;


import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.assessment.Assessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessorRepository extends SearchRepository<Assessor, Integer> {

    @Query("select c from Assessor c where concat(c.email, ' ', c.name, ' ', c.title) like %?1%")
    public Page<Assessor> findAll(String keyword, Pageable pageable);

    @Query("update Assessor c set c.enabled = ?2 where c.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);

    @Query("select c from Assessor c where c.email = ?1")
    public Assessor findByEmail(String email);

    public Long countById(Integer id);

    @Query("select a from Assessor a where a.enabled = true")
    public List<Assessor> getAssessorsByEnabled();

}

