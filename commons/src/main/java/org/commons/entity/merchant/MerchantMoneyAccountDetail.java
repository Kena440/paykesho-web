package org.commons.entity.merchant;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "merchant_money_account_details")
public class MerchantMoneyAccountDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
    @Column(name = "account_number", length = 50, nullable = false, unique = true)
    private String accountNumber;
    @Column(name = "financial_institution", length = 128, nullable = false)
    private String financialInstitution;
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;
}
