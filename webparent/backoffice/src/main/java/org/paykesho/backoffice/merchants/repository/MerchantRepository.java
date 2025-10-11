package org.paykesho.backoffice.merchants.repository;

import org.commons.entity.merchant.Merchant;
import org.commons.enums.MerchantType;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends SearchRepository<Merchant, Integer> {
    @Query("select m from Merchant m where m.email = :email")
    public Merchant getMerchantByEmail(@Param("email") String email);

    @Query("select m from Merchant m where m.name = :name")
    public Merchant getMerchantByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from Merchant m where concat(m.name, ' ', m.latitude, ' ', m.servicesOffered, ' ', m.email) like %?1%")
    public Page<Merchant> findAll(String keyword, Pageable pageable);

    @Query("update Merchant m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select count(c.id) from Merchant c")
    public Long countAllMerchants();

    @Query("select m from Merchant m where m.id = :id and m.name = :merchantName and m.merchantType = :merchantType")
    public Merchant getMerchantByIdAndNameAndMerchantType(@Param("id") Integer id, @Param("merchantName") String merchantName, @Param("merchantType") MerchantType merchantType);

    @Query("select m " + "from Merchant m " + "where m.enabled = ?1 ")
    List<Merchant> findMerchantTypesEnabled(boolean enabled);

    // Additional methods for dashboard analytics
    long countByEnabledTrue();

    long countByEnabledFalse();

    long countByRegistrationDateAfter(java.util.Date date);

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================
    
    /**
     * Find merchant by UUID for public/URL access
     * @param uuid The merchant's UUID
     * @return Optional containing the merchant if found
     */
    Optional<Merchant> findByUuid(String uuid);
    
    /**
     * Check if a merchant exists with the given UUID
     * @param uuid The merchant's UUID
     * @return true if merchant exists, false otherwise
     */
    @Query("SELECT COUNT(m) > 0 FROM Merchant m WHERE m.uuid = ?1")
    boolean existsByUuid(String uuid);

}
