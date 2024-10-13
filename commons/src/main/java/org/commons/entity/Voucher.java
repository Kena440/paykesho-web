package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.client.Client;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "amount", length = 128, nullable = false, unique = false)
    private BigDecimal amount;
    @Column(name = "available_amount", length = 128, nullable = false, unique = false)
    private BigDecimal availableAmount;

    @Column(name = "repayment_amount", length = 128, nullable = true, unique = false)
    private BigDecimal repaymentAmount;

    @Column(name = "interest_amount", length = 128, nullable = true, unique = false)
    private float interestPercent;

    @Column(name = "repayment_cyle", length = 128, nullable = true, unique = false)
    private String repaymentCycle;

    @Column(name = "repayment_date", length = 128, nullable = true, unique = false)
    private Date repaymentDate;

    @Column(name = "request_date", length = 128, nullable = false, unique = false)
    private Date requestDate;

    @Column(name = "reason", length = 128, nullable = false)
    private String reason;

    @Column(length = 64)
    private String payslip;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    private String payslipPath;

    private boolean approved;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "history",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "history_id")
    )
    private Set<VoucherTrxHistory> voucherTrxHistory = new HashSet<>();

    public void addHistoryData(VoucherTrxHistory history) {
        this.voucherTrxHistory.add(history);
    }
}
