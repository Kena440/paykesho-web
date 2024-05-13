package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payment_account")
public class PaymentAccount extends IdBasedEntity {
    @Column(length = 64, nullable = false, name = "bank_name")
    private String bankName;
    @Column(length = 64, nullable = false, name = "account_name")
    private String accountName;
    @Column(length = 64, nullable = false, name = "sort_code")
    private String sortCode;
    @Column(length = 64, nullable = false, unique = true, name = "account_no")
    private String accountNo;
    @Column(length = 64, nullable = false)
    private String branch;


    @Override
    public String toString() {
        return "Account Name: " + accountName + "\nBank: " + bankName + "\nAccount No.: " + accountNo + "\nBranch.: " + branch + "\nSort Code: " + sortCode;
    }
}
