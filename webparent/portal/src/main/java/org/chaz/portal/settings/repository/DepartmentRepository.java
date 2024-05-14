package org.chaz.portal.settings.repository;

import org.commons.entity.settings.Department;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DepartmentRepository extends CrudRepository<Department, Integer> {
    public List<Department> findAllByOrderByDepartmentNameDesc();

    @Query("select d from Department d where d.departmentCode = ?1")
    public Department findByCode(String code);

    @Query("select d from Department d where d.departmentName = ?1")
    public Department findByName(String name);
}
