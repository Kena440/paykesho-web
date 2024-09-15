package org.commons.entity.merchant;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.enums.SettlementStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @OneToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "settlement_amount", nullable = false)
    private BigDecimal settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 64)
    private SettlementStatus settlementStatus;

    @Column(name = "settlement_date")
    private Date settlementDate;

    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @PrePersist
    public void prePersist() {
        lastUpdated = new Date();
        if (settlementDate == null) {
            settlementDate = new Date();
        }
    }
}
