package org.paykesho.backoffice.utils;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.merchants.repository.MerchantRepository;
import org.paykesho.backoffice.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.backoffice.settlement.repository.SettlementRepository;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.paykesho.backoffice.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;

@Slf4j
@Service
public class PaykeshoReferenceService {

    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private VouchersRepository vouchersRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private SettlementRepository settlementRepository;
    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;

    public String generateCode(String itemRequest) {
        String reference = "";
        String itemPrefix = "";
        Long count;

        switch (itemRequest) {
            case "ITEM_REQ_MNT":
                itemPrefix = "M";
                count = merchantRepository.countAllMerchants() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_CLT":
                itemPrefix = "C";
                count = clientRepository.countAllClients() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_VCR":
                itemPrefix = "V";
                count = vouchersRepository.countAllVouchers() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_TRX":
                itemPrefix = "T";
                count = transactionRepository.countAllTransactions() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_STL":
                itemPrefix = "S";
                count = settlementRepository.countAllSettlements() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_RPS":
                itemPrefix = "R";
                count = repaymentScheduleRepository.countAllRepaymentSchedules() + 1;
                reference = getReference(itemPrefix, count);
                break;

            default:

                break;
        }

        return reference;
    }

    private String getReference(String itemPrefix, Long count) {
        String reference;

        Date dt = new Date();
        int year = dt.getYear();
        int currentYear = year + 1900;

        String randomCode = RandomString.make(5).toUpperCase(Locale.ENGLISH);
        reference = itemPrefix + currentYear + "" + count + randomCode;
        return reference;
    }

}
