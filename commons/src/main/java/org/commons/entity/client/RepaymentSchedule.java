package org.commons.entity.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.Voucher;
import org.commons.enums.PaymentStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "repayment_schedules")
public class RepaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "amount_due", length = 128, nullable = false)
    private BigDecimal amountDue;
    @Column(name = "amount_paid", length = 128, nullable = false)
    private BigDecimal amountPaid;
    @Column(name = "due_date", length = 128, nullable = false)
    private Date dueDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private PaymentStatus status;
}
