package org.commons.entity.client;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/** PIN credential row. PINs are stored only as adaptive password hashes. */
@Entity
@Table(name = "client_transaction_pins")
public class ClientTransactionPin {
    @Id
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_changed_at", nullable = false)
    private Instant lastChangedAt;

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public short getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(short failedAttempts) { this.failedAttempts = failedAttempts; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public Instant getLastChangedAt() { return lastChangedAt; }
    public void setLastChangedAt(Instant lastChangedAt) { this.lastChangedAt = lastChangedAt; }
}
