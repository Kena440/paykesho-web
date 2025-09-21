package org.paykesho.merchantportal.merchants.repository;

import org.commons.entity.merchant.Merchant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MerchantRepository extends CrudRepository<Merchant, Integer> {
    @Query("select m from Merchant m where m.email = :email")
    public Merchant getMerchantByEmail(@Param("email") String email);

    @Query("select m from Merchant m where m.name = :name")
    public Merchant getMerchantByName(@Param("name") String name);

    @Query("select m from Merchant m where m.contactPerson.id = :id")
    public Merchant getMerchantByContactPersonId(@Param("id") Integer id);

    public Long countById(Integer id);

    @Query("update Merchant m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Merchant m where m.id = :id and m.name = :memberName and m.email = :email")
    public Merchant getMerchantByIdAndNameAndMemberTypeAndEmail(@Param("id") Integer id, @Param("memberName") String memberName, @Param("email") String email);

    @Query("select m " + "from Merchant m " + "where m.enabled = ?1 ")
    List<Merchant> findMerchantEnabled(boolean enabled);

    @Query("select m " + "from Merchant m " + "where m.enabled = ?1 ")
    List<Merchant> findMerchantTypesEnabled(boolean enabled);

}
