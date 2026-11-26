package org.paykesho.paymentservice.repository;

import org.commons.entity.ProviderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderEventRepository extends JpaRepository<ProviderEvent, Long> {
    @Modifying
    @Query(value = "insert into app.provider_events(provider,event_id,event_type,reference,payload,signature_valid) " +
            "values(:provider,:eventId,:eventType,:reference,cast(:payload as jsonb),:signatureValid) " +
            "on conflict(provider,event_id) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("provider") String provider, @Param("eventId") String eventId,
                      @Param("eventType") String eventType, @Param("reference") String reference,
                      @Param("payload") String payload, @Param("signatureValid") boolean signatureValid);

    @Modifying
    @Query(value = "update app.provider_events set processed_at=now() where provider=:provider and event_id=:eventId", nativeQuery = true)
    int markProcessed(@Param("provider") String provider, @Param("eventId") String eventId);
}
