package org.chaz.portal.assessments.repository;

import org.chaz.portal.paging.SearchRepository;
import org.commons.entity.assessment.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentsRepository extends SearchRepository<Assessment, Integer> {

    @Query("select e from Assessment e where e.name = :name")
    public Assessment getAssessmentByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("update Assessment e set e.enabled = ?2 where e.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    //@Query(value = "SELECT e FROM Assessment e WHERE DATEDIFF(e.eventDate, CURRENT_DATE) <= :days")
    @Query("SELECT e FROM Assessment e WHERE e.assessmentDate > CURRENT_DATE AND DATEDIFF(e.assessmentDate, CURRENT_DATE) <= :days")
    public List<Assessment> findUpcomingAssessments(@Param("days") int days);

    @Query("select distinct t from Assessment t, AssessmentAssessor aa where t.id = aa.assessment.id and aa.assessor.id = ?2 and (t.name like %?1% or t.details like %?1%) ")
    public Page<Assessment> findAll(String keyword, Integer assessorId, Pageable pageable);

    @Query("select t from Assessment t, AssessmentAssessor aa where t.id = aa.assessment.id and aa.assessor.id = ?1")
    public Page<Assessment> findAll(Integer assessorId, Pageable pageable);

    @Query("select e from Assessment e where concat(e.name, ' ', e.details) like %?1%")
    public Page<Assessment> findAll(String keyword, Pageable pageable);
}
