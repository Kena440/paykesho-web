package org.paykesho.clientportal.kyc;

import org.commons.entity.kyc.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
}
