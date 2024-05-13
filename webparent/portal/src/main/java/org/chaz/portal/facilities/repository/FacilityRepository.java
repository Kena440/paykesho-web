package org.chaz.portal.facilities.repository;

import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.enums.MemberType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberInstitutionRepository extends CrudRepository<MemberInstitution, Integer> {
    @Query("select m from MemberInstitution m where m.email = :email")
    public MemberInstitution getMemberInstitutionByEmail(@Param("email") String email);

    @Query("select m from MemberInstitution m where m.name = :name")
    public MemberInstitution getMemberInstitutionByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("update MemberInstitution m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from MemberInstitution m where m.id = :id and m.province.id = :provinceId and m.name = :memberName and m.memberType = :memberType and m.email = :email")
    public MemberInstitution getMemberInstitutionByIdAndProvinceAndNameAndMemberTypeAndEmail(@Param("id") Integer id, @Param("provinceId") Integer provinceId, @Param("memberName") String memberName, @Param("memberType") MemberType memberType, @Param("email") String email);

    @Query("select m " + "from MemberInstitution m " + "where m.enabled = ?1 ")
    List<MemberInstitution> findMemberTypesEnabled(boolean enabled);

    @Query("select SUM(m.catchmentPopulation) as total from MemberInstitution m where m.enabled = ?1")
    public Long getTotalCatchmentMembers(boolean enabled);

    @Query("select m from MemberInstitution m where m.contactPerson.id = :id")
    public MemberInstitution getMemberInstitutionByContactPerson(Integer id);
}
