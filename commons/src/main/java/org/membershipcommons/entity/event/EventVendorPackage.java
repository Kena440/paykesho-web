package org.membershipcommons.entity.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.settings.Currency;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_vendor_package")
public class EventVendorPackage extends IdBasedEntity {
    @Column(length = 64, nullable = false, name = "package_name")
    private String packageName;
    @Column(nullable = false, name = "package_details")
    private String packageDetails;
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @Column(name = "package_price", nullable = false)
    private float packagePrice;

    @Override
    public String toString() {
        return packageName + " - " + currency.getSymbol() + " " + packagePrice;
    }
}
