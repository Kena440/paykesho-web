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
@Table(name = "employee_payslip_information")
public class EmployeePayslipInformation extends IdBasedEntity {
    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "gross_pay", length = 128, nullable = false)
    private BigDecimal grossPay;
    @Column(name = "net_pay", length = 128, nullable = false)
    private BigDecimal netPay;
    private Date updateDate;
}
