package org.paykesho.backoffice.kyc;

import org.commons.entity.kyc.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByKycApplicationIdOrderByIdAsc(Long kycApplicationId);
}
