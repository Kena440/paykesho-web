package org.paykesho.backoffice.settlement.service;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantMoneyAccountDetail;
import org.commons.entity.merchant.Settlement;
import org.commons.enums.SettlementStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.settlement.repository.MerchantMoneyAccountDetailsRepository;
import org.paykesho.backoffice.settlement.repository.SettlementRepository;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@Service
public class SettlementService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private MerchantMoneyAccountDetailsRepository merchantMoneyAccountDetailsRepository;

    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;

    public List<Settlement> getAllSettlements() {
        return settlementRepository.getAllSettlements();
    }

    public List<Settlement> listAllByMerchant(Merchant merchant) {
        return settlementRepository.getSettlementsByMerchant(merchant);
    }

    public void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
        List<Transaction> unsettledTransactions = transactionRepository.findUnsettledTransactionsByMerchant(merchant);

        BigDecimal totalAmount = unsettledTransactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_STL");
            if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

            Settlement settlement = new Settlement();
            settlement.setReference(paykeshoRef);
            settlement.setMerchant(merchant);
            settlement.setSettlementAmount(totalAmount);
            settlement.setSettlementStatus(SettlementStatus.PENDING);
            settlementRepository.save(settlement);

            boolean paymentSuccessful = initiatePayment(merchant, settlement);

            if (paymentSuccessful) {
                settlement.setSettlementStatus(SettlementStatus.COMPLETED);
                markTransactionsAsSettled(unsettledTransactions);
            } else {
                settlement.setSettlementStatus(SettlementStatus.FAILED);
            }

            settlementRepository.save(settlement);
        }
    }

    private boolean initiatePayment(Merchant merchant, Settlement settlement) {
        try {
            MerchantMoneyAccountDetail detail = merchantMoneyAccountDetailsRepository.getMerchantMoneyAccountDetailByMerchantId(merchant.getId());

            URL url = new URL("http://payments.paykesho.com/payments-api");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("API-Key", "F39E25A3B2A446F98E87A95723AE4");
            connection.setDoOutput(true);

            String jsonPayload = "{" + "\"amount\": \"" + settlement.getSettlementAmount() + "\"," + "\"reference_no\": \"" + settlement.getReference() + "\"," + "\"bank_name\": \"" + detail.getFinancialInstitution() + "\"," + "\"receiver_id\": \"" + detail.getAccountNumber() + "\"" + "}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8);
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                return response.contains("\"response_code\": \"100\"");
            } else {
                System.out.println("Payment failed with response code: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void markTransactionsAsSettled(List<Transaction> transactions) {
        for (Transaction txn : transactions) {
            txn.setSettled(true);
            transactionRepository.save(txn);
        }
    }
}
