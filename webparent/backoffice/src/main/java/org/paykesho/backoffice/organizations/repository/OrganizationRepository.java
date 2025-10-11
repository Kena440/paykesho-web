package org.paykesho.backoffice.organizations.repository;

import org.commons.entity.organization.Organization;
import org.commons.enums.OrganizationType;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends SearchRepository<Organization, Integer> {
    @Query("select m from Organization m where m.email = :email")
    public Organization getOrganizationByEmail(@Param("email") String email);

    @Query("select m from Organization m where m.name = :name")
    public Organization getOrganizationByName(@Param("name") String name);

    public Long countById(Integer id);

    @Query("select m from Organization m where concat(m.name, ' ', m.latitude, ' ', m.organizationType, ' ', m.email) like %?1%")
    public Page<Organization> findAll(String keyword, Pageable pageable);

    @Query("update Organization m set m.enabled = ?2 where m.id = ?1")
    @Modifying
    public void updateEnableStatus(Integer id, boolean enabled);

    @Query("select m from Organization m where m.id = :id and m.name = :organizationName and m.organizationType = :organizationType")
    public Organization getOrganizationByIdAndNameAndOrganizationType(@Param("id") Integer id, @Param("organizationName") String organizationName, @Param("organizationType") OrganizationType organizationType);

    @Query("select m " + "from Organization m " + "where m.enabled = ?1 ")
    List<Organization> findOrganizationTypesEnabled(boolean enabled);

    // Additional methods for dashboard analytics
    long countByEnabledTrue();

    long countByEnabledFalse();

    long countByRegistrationDateAfter(java.util.Date date);

    // ============================================================================
    // UUID-based methods for secure URL access
    // ============================================================================
    
    /**
     * Find organization by UUID for public/URL access
     * @param uuid The organization's UUID
     * @return Optional containing the organization if found
     */
    Optional<Organization> findByUuid(String uuid);
    
    /**
     * Check if an organization exists with the given UUID
     * @param uuid The organization's UUID
     * @return true if organization exists, false otherwise
     */
    @Query("SELECT COUNT(o) > 0 FROM Organization o WHERE o.uuid = ?1")
    boolean existsByUuid(String uuid);

}
