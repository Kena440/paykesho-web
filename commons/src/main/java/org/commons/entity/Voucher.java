package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.client.Client;
import org.commons.enums.VoucherStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

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

    private String payslip;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    private String payslipPath;
    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_status", length = 50)
    private VoucherStatus voucherStatus;
    private boolean approved;

}
