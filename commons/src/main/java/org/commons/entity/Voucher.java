package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.commons.entity.client.Client;
import org.commons.enums.VoucherStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "vouchers")
public class Voucher extends IdBasedEntity {
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "amount", length = 128, nullable = false)
    private BigDecimal amount;
    @Column(name = "available_amount", length = 128, nullable = false)
    private BigDecimal availableAmount;

    @Column(name = "repayment_amount", length = 128)
    private BigDecimal repaymentAmount;

    // Interest rate in percent (e.g. 15.0000). Stored as exact decimal, never float.
    // The column keeps its historical name "interest_amount".
    @Column(name = "interest_amount", precision = 9, scale = 4)
    private BigDecimal interestPercent;

    @Column(name = "repayment_cyle", length = 128)
    private Integer repaymentCycle;

    @Column(name = "first_repayment_date", length = 128)
    private Date firstRepaymentDate;

    @Column(name = "last_repayment_date", length = 128)
    private Date lastRepaymentDate;


    @Column(name = "request_date", length = 128, nullable = false)
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
