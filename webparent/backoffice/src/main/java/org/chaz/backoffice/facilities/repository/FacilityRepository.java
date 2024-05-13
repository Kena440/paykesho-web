package org.chaz.backoffice.facilities.repository;

import org.chaz.membershipbackoffice.paging.SearchRepository;
import org.membershipcommons.dto.GeneralStatsDto;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.enums.MemberType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberInstitutionRepository extends SearchRepository<MemberInstitution, Integer> {
    @Query("select m from MemberInstitution m where m.email = :email")
    public MemberInstitution getMemberInstitutionByEmail(@Param("email") String email);

    @Query("select m from MemberInstitution m where m.name = :name")
    public MemberInstitution getMemberInstitutionByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from MemberInstitution m where concat(m.name, ' ', m.denomination.name, ' ', m.levelOfCare.name, ' ', m.district, ' ', m.province.name, ' ', m.servicesOffered) like %?1%")
    public Page<MemberInstitution> findAll(String keyword, Pageable pageable);

    @Query("update MemberInstitution m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from MemberInstitution m where m.id = :id and m.province.id = :provinceId and m.name = :memberName and m.memberType = :memberType")
    public MemberInstitution getMemberInstitutionByIdAndProvinceAndNameAndMemberType(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("memberName") String memberName, @Param("memberType") MemberType memberType);

    @Query("select m " + "from MemberInstitution m " + "where m.enabled = ?1 ")
    List<MemberInstitution> findMemberTypesEnabled(boolean enabled);

    @Query("select SUM(m.catchmentPopulation) as total from MemberInstitution m where m.enabled = ?1")
    public Long getTotalCatchmentMembers(boolean enabled);

    @Query("select m from MemberInstitution m where m.levelOfCare.id = ?1 and m.enabled = ?2 ")
    List<MemberInstitution> findMemberInstitutionsByLevelOfCareIdAndEnabled(Integer id, boolean enabled);

    @Query("select new org.membershipcommons.dto.GeneralStatsDto(s.name, count(m.province.id)) from State s, MemberInstitution m where m.province.id = s.id and s.country.id = ?1 and m.enabled = ?2 group by s.name order by s.name")
    List<GeneralStatsDto> getMembersCountByProvince(@Param("countryId") Integer countryId, boolean enabled);

    @Query("select new org.membershipcommons.dto.GeneralStatsDto(d.name, count(m.denomination.id)) from Denomination d, MemberInstitution m, Country c where m.denomination.id = d.id and c.id in (select s.country.id from State s where s.country.id = ?1) and m.enabled = ?2 group by d.name order by d.name")
    List<GeneralStatsDto> getMembersCountByDenomination(@Param("countryId") Integer countryId, boolean enabled);

    @Query("select new org.membershipcommons.dto.GeneralStatsDto(l.name, count(m.levelOfCare.id)) from LevelOfCare l, MemberInstitution m, Country c where m.levelOfCare.id = l.id and c.id in (select s.country.id from State s where s.country.id = ?1) and m.enabled = ?2 group by l.name order by l.name")
    List<GeneralStatsDto> getMembersCountByLevelOfCare(@Param("countryId") Integer countryId, boolean enabled);
/*
    @Query("select p from MemberInstitution p where p.category.id in (select mn from MemberNeeds) = ?1 or p.category.allParentIds like %?2%) and (p.name like %?3% or p.phoneNumber like %?3%)")
    Page<MemberInstitution> searchInNeedType(Integer needTypeId, String needTypeIdMatch, String keyword, Pageable pageable);*/
}
