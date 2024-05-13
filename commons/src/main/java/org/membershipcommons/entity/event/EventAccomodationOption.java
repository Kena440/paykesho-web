package org.membershipcommons.entity.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.settings.Currency;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_accomodation_options")
public class EventAccomodationOption extends IdBasedEntity {
    @Column(nullable = false, name = "accomodation_name")
    private String accomodationName;
    @Column(nullable = false, name = "accomodation_details")
    private String accomodationDetails;
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @Column(name = "accomodation_price", nullable = false)
    private float accomodationPrice;

    @Override
    public String toString() {
        return accomodationName + " - " + currency.getSymbol() + "" + accomodationPrice;
    }
}
