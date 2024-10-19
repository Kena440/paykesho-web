package org.paykesho.clientportal.organizations.repository;

import org.commons.entity.organization.Organization;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationRepository extends CrudRepository<Organization, Integer> {
    @Query("select m from Organization m where m.email = :email")
    public Organization getOrganizationByEmail(@Param("email") String email);

    @Query("select m from Organization m where m.name = :name")
    public Organization getOrganizationByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("update Organization m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Organization m where m.id = :id and m.name = :memberName and m.email = :email")
    public Organization getOrganizationByIdAndNameAndMemberTypeAndEmail(@Param("id") Integer id, @Param("memberName") String memberName, @Param("email") String email);

    @Query("select m " + "from Organization m " + "where m.enabled = ?1 ")
    List<Organization> findOrganizationEnabled(boolean enabled);

    @Query("select m " + "from Organization m " + "where m.enabled = ?1 ")
    List<Organization> findOrganizationTypesEnabled(boolean enabled);

}
