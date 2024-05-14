package org.chaz.backoffice.facilities.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.dto.GeneralStatsDto;
import org.commons.entity.facilities.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacilityRepository extends SearchRepository<Facility, Integer> {
    @Query("select m from Facility m where m.email = :email")
    public Facility getFacilityByEmail(@Param("email") String email);

    @Query("select m from Facility m where m.name = :name")
    public Facility getFacilityByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from Facility m where concat(m.name, ' ', m.latitude, ' ', m.levelOfCare.name, ' ', m.district, ' ', m.province.name, ' ', m.email) like %?1%")
    public Page<Facility> findAll(String keyword, Pageable pageable);

    @Query("update Facility m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Facility m where m.id = :id and m.province.id = :provinceId and m.name = :facilityName and m.levelOfCare.id = :levelOfCareId")
    public Facility getFacilityByIdAndProvinceAndNameAndLevelOfCare(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("facilityName") String facilityName, @Param("levelOfCareId") Integer levelOfCareId);

    @Query("select m " + "from Facility m " + "where m.enabled = ?1 ")
    List<Facility> findFacilityTypesEnabled(boolean enabled);
    @Query("select m from Facility m where m.levelOfCare.id = ?1 and m.enabled = ?2 ")
    List<Facility> findFacilitysByLevelOfCareIdAndEnabled(Integer id, boolean enabled);

    @Query("select new org.commons.dto.GeneralStatsDto(s.name, count(m.province.id)) from State s, Facility m where m.province.id = s.id and s.country.id = ?1 and m.enabled = ?2 group by s.name order by s.name")
    List<GeneralStatsDto> getFacilitysCountByProvince(@Param("countryId") Integer countryId, boolean enabled);

    @Query("select new org.commons.dto.GeneralStatsDto(l.name, count(m.levelOfCare.id)) from LevelOfCare l, Facility m, Country c where m.levelOfCare.id = l.id and c.id in (select s.country.id from State s where s.country.id = ?1) and m.enabled = ?2 group by l.name order by l.name")
    List<GeneralStatsDto> getFacilitysCountByLevelOfCare(@Param("countryId") Integer countryId, boolean enabled);

}
