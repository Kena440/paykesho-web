package org.chaz.backoffice.csos.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.dto.GeneralStatsDto;
import org.commons.entity.cso.CivilSocietyOrganization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CSORepository extends SearchRepository<CivilSocietyOrganization, Integer> {
    @Query("select m from CivilSocietyOrganization m where m.email = :email")
    public CivilSocietyOrganization getCivilSocietyOrganizationByEmail(@Param("email") String email);

    @Query("select m from CivilSocietyOrganization m where m.name = :name")
    public CivilSocietyOrganization getCivilSocietyOrganizationByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from CivilSocietyOrganization m where concat(m.name, ' ', m.latitude, ' ', m.servicesOffered, ' ', m.district, ' ', m.province.name, ' ', m.email) like %?1%")
    public Page<CivilSocietyOrganization> findAll(String keyword, Pageable pageable);

    @Query("update CivilSocietyOrganization m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from CivilSocietyOrganization m where m.id = :id and m.province.id = :provinceId and m.name = :facilityName and m.district = :district")
    public CivilSocietyOrganization getCivilSocietyOrganizationByIdAndProvinceAndNameAndDistrict(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("facilityName") String facilityName, @Param("district") String district);

    @Query("select m " + "from CivilSocietyOrganization m " + "where m.enabled = ?1 ")
    List<CivilSocietyOrganization> findCivilSocietyOrganizationTypesEnabled(boolean enabled);

    @Query("select new org.commons.dto.GeneralStatsDto(s.name, count(m.province.id)) from State s, CivilSocietyOrganization m where m.province.id = s.id and s.country.id = ?1 and m.enabled = ?2 group by s.name order by s.name")
    List<GeneralStatsDto> getCivilSocietyOrganizationsCountByProvince(@Param("countryId") Integer countryId, boolean enabled);

}
