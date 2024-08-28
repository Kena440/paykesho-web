package org.paykesho.backoffice.organizations.repository;

import org.commons.entity.organization.EmployeeRecord;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRecordRepository extends SearchRepository<EmployeeRecord, Integer> {
    @Query("select m from EmployeeRecord m where m.manNumber = :manNumber")
    public EmployeeRecord getEmployeeRecordByManNumberl(@Param("manNumber") String manNumber);

    @Query("select m from EmployeeRecord m where m.nrc = :nrc")
    public EmployeeRecord getEmployeeRecordByNrc(@Param("nrc") String nrc);

    public Long countById(Integer id);

    public List<EmployeeRecord> getEmployeeRecordsByOrganizationId(Integer id);

    @Query("select m from EmployeeRecord m where concat(m.name, ' ', m.nrc, ' ', m.tpin, ' ', m.manNumber) like %?1%")
    public Page<EmployeeRecord> findAll(String keyword, Pageable pageable);

}
