package org.paykesho.backoffice.merchants.repository;

import org.commons.dto.GeneralStatsDto;
import org.commons.entity.merchant.Merchant;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MerchantRepository extends SearchRepository<Merchant, Integer> {
    @Query("select m from Merchant m where m.email = :email")
    public Merchant getCivilSocietyOrganizationByEmail(@Param("email") String email);

    @Query("select m from Merchant m where m.name = :name")
    public Merchant getCivilSocietyOrganizationByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from Merchant m where concat(m.name, ' ', m.latitude, ' ', m.servicesOffered, ' ', m.district, ' ', m.province.name, ' ', m.email) like %?1%")
    public Page<Merchant> findAll(String keyword, Pageable pageable);

    @Query("update Merchant m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Merchant m where m.id = :id and m.province.id = :provinceId and m.name = :facilityName and m.district = :district")
    public Merchant getCivilSocietyOrganizationByIdAndProvinceAndNameAndDistrict(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("facilityName") String facilityName, @Param("district") String district);

    @Query("select m " + "from Merchant m " + "where m.enabled = ?1 ")
    List<Merchant> findCivilSocietyOrganizationTypesEnabled(boolean enabled);

    @Query("select new org.commons.dto.GeneralStatsDto(s.name, count(m.province.id)) from State s, Merchant m where m.province.id = s.id and s.country.id = ?1 and m.enabled = ?2 group by s.name order by s.name")
    List<GeneralStatsDto> getCivilSocietyOrganizationsCountByProvince(@Param("countryId") Integer countryId, boolean enabled);

}
