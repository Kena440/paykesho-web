package org.paykesho.backoffice.kyc;

import org.commons.entity.kyc.ClientKycApplication;
import org.commons.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KycApplicationRepository extends JpaRepository<ClientKycApplication, Long> {

    @Query("select a from ClientKycApplication a join fetch a.client c left join fetch c.organization " +
           "where a.status in ?1 order by a.submittedAt asc")
    List<ClientKycApplication> findByStatusInOldestFirst(Collection<KycStatus> statuses);

    @Query("select a from ClientKycApplication a join fetch a.client c left join fetch c.organization " +
           "where a.status in ?1 order by a.reviewedAt desc")
    List<ClientKycApplication> findDecided(Collection<KycStatus> statuses);

    @Query("select a from ClientKycApplication a join fetch a.client c left join fetch c.organization where a.id = ?1")
    Optional<ClientKycApplication> findWithClient(Long id);

    Optional<ClientKycApplication> findFirstByClient_IdOrderBySubmittedAtDesc(Integer clientId);

    long countByStatusIn(Collection<KycStatus> statuses);
}
