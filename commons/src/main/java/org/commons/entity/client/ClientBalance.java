package org.commons.entity.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "client_balance")
public class ClientBalance extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "current_balance", length = 128, nullable = false)
    private BigDecimal currentBalance;
    @Column(name = "available_balance", length = 128, nullable = false)
    private BigDecimal availableBalance;
    private Date lastUpdate;

    @Override
    public String toString() {
        return availableBalance + " [" + lastUpdate + "]";
    }
}
