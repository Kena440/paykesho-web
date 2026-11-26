package org.paykesho.clientportal.kyc;

import org.commons.entity.kyc.ClientKycApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientKycApplicationRepository extends JpaRepository<ClientKycApplication, Long> {
    Optional<ClientKycApplication> findFirstByClient_IdOrderBySubmittedAtDesc(Integer clientId);
}
