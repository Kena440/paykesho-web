package org.commons.entity.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.enums.ProcessingStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payroll_deductions")
public class PayrollDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "amount_deducted", length = 128, nullable = false)
    private BigDecimal amountDeducted;
    @Column(name = "payroll_date", length = 128, nullable = false)
    private Date payrollDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private ProcessingStatus status;
}
