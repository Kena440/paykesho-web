package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @OneToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;
    @OneToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;
    @Column(name = "amount", length = 128, nullable = false)
    private BigDecimal amount;
    @Column(name = "naration", length = 128, nullable = false)
    private String naration;
    @Column(name = "txn_date", length = 128, nullable = false)
    private Date txnDate;

    private boolean settled;

}
