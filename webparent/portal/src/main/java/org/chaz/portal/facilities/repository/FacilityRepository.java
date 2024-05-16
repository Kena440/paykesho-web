package org.chaz.portal.facilities.repository;

import org.commons.entity.facilities.Facility;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacilityRepository extends CrudRepository<Facility, Integer> {
    @Query("select m from Facility m where m.email = :email")
    public Facility getFacilityByEmail(@Param("email") String email);

    @Query("select m from Facility m where m.name = :name")
    public Facility getFacilityByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("update Facility m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Facility m where m.id = :id and m.province.id = :provinceId and m.name = :memberName and m.email = :email")
    public Facility getFacilityByIdAndProvinceAndNameAndMemberTypeAndEmail(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("memberName") String memberName, @Param("email") String email);

    @Query("select m " + "from Facility m " + "where m.enabled = ?1 ")
    List<Facility> findFacilityEnabled(boolean enabled);



}
