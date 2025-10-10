package org.commons.entity.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "client_balance")
public class ClientBalance extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "current_balance", length = 128)
    private BigDecimal currentBalance;

    @Column(name = "available_balance", length = 128)
    private BigDecimal availableBalance;

    @Column(name = "held_balance", length = 128)
    private BigDecimal heldBalance;

    @Column(name = "total_credits", length = 128)
    private BigDecimal totalCredits;

    @Column(name = "total_debits", length = 128)
    private BigDecimal totalDebits;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "last_transaction_id", length = 128)
    private String lastTransactionId;

    @Column(name = "last_transaction_type", length = 64)
    private String lastTransactionType;

    @Column(name = "last_transaction_amount", length = 128)
    private BigDecimal lastTransactionAmount;

    @Column(name = "last_transaction_date")
    private Date lastTransactionDate;

    private Date lastUpdate;

    /**
     * Version field for optimistic locking to prevent concurrent balance update conflicts
     * This will automatically increment on each update and throw OptimisticLockException
     * if another transaction has modified the balance in the meantime
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public String toString() {
        return availableBalance + " [" + lastUpdate + "]";
    }
}