package org.paykesho.organizationportal.transactions.repository;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Basic queries
    List<Transaction> findByClient(Client client);

    List<Transaction> findByMerchant(Merchant merchant);

    // Organization-based queries
    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization")
    List<Transaction> findByOrganization(@Param("organization") Organization organization);

    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true")
    List<Transaction> findByOrganizationAndSettledTrue(@Param("organization") Organization organization);

    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.settled = false")
    List<Transaction> findByOrganizationAndSettledFalse(@Param("organization") Organization organization);

    // Date-based queries for organization
    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate >= :startDate")
    List<Transaction> findByOrganizationAndTxnDateAfter(@Param("organization") Organization organization, @Param("startDate") Date startDate);

    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByOrganizationAndTxnDateBetween(@Param("organization") Organization organization,
                                                          @Param("startDate") Date startDate,
                                                          @Param("endDate") Date endDate);

    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate >= :startDate AND t.settled = true")
    List<Transaction> findByOrganizationAndTxnDateAfterAndSettledTrue(@Param("organization") Organization organization, @Param("startDate") Date startDate);

    @Query("SELECT t FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate BETWEEN :startDate AND :endDate AND t.settled = true")
    List<Transaction> findByOrganizationAndTxnDateBetweenAndSettledTrue(@Param("organization") Organization organization,
                                                                        @Param("startDate") Date startDate,
                                                                        @Param("endDate") Date endDate);

    // Count queries for organization
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization = :organization")
    long countByOrganization(@Param("organization") Organization organization);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true")
    long countByOrganizationAndSettledTrue(@Param("organization") Organization organization);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization = :organization AND t.settled = false")
    long countByOrganizationAndSettledFalse(@Param("organization") Organization organization);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate >= :startDate")
    long countByOrganizationAndTxnDateAfter(@Param("organization") Organization organization, @Param("startDate") Date startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate BETWEEN :startDate AND :endDate")
    long countByOrganizationAndTxnDateBetween(@Param("organization") Organization organization,
                                              @Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    // Client-specific queries
    @Query("SELECT t FROM Transaction t WHERE t.client = :client AND t.txnDate >= :startDate")
    List<Transaction> findByClientAndTxnDateAfter(@Param("client") Client client, @Param("startDate") Date startDate);

    @Query("SELECT t FROM Transaction t WHERE t.client = :client AND t.txnDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByClientAndTxnDateBetween(@Param("client") Client client,
                                                    @Param("startDate") Date startDate,
                                                    @Param("endDate") Date endDate);

    @Query("SELECT t FROM Transaction t WHERE t.client = :client AND t.txnDate >= :startDate AND t.settled = true")
    List<Transaction> findByClientAndTxnDateAfterAndSettledTrue(@Param("client") Client client, @Param("startDate") Date startDate);

    @Query("SELECT t FROM Transaction t WHERE t.client = :client AND t.txnDate BETWEEN :startDate AND :endDate AND t.settled = true")
    List<Transaction> findByClientAndTxnDateBetweenAndSettledTrue(@Param("client") Client client,
                                                                  @Param("startDate") Date startDate,
                                                                  @Param("endDate") Date endDate);

    // Count queries for client
    long countByClient(Client client);

    long countByClientAndSettledTrue(Client client);

    long countByClientAndSettledFalse(Client client);

    long countByClientAndTxnDateAfter(Client client, Date startDate);

    long countByClientAndTxnDateBetween(Client client, Date startDate, Date endDate);

    // Analytics queries
    @Query("SELECT t.client.id, t.client.name, COUNT(t) as transactionCount " +
            "FROM Transaction t WHERE t.client.organization = :organization " +
            "GROUP BY t.client.id, t.client.name " +
            "ORDER BY transactionCount DESC")
    List<Object[]> findTopClientsByTransactionCount(@Param("organization") Organization organization);

    @Query("SELECT t.client.id, t.client.name, SUM(t.amount) as totalValue " +
            "FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true " +
            "GROUP BY t.client.id, t.client.name " +
            "ORDER BY totalValue DESC")
    List<Object[]> findTopClientsByTransactionValue(@Param("organization") Organization organization);

    @Query("SELECT t.merchant.id, t.merchant.name, COUNT(t) as transactionCount " +
            "FROM Transaction t WHERE t.client.organization = :organization " +
            "GROUP BY t.merchant.id, t.merchant.name " +
            "ORDER BY transactionCount DESC")
    List<Object[]> findTopMerchantsByTransactionCount(@Param("organization") Organization organization);

    @Query("SELECT t.merchant.id, t.merchant.name, SUM(t.amount) as totalValue " +
            "FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true " +
            "GROUP BY t.merchant.id, t.merchant.name " +
            "ORDER BY totalValue DESC")
    List<Object[]> findTopMerchantsByTransactionValue(@Param("organization") Organization organization);

    // Revenue and volume queries
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true")
    java.math.BigDecimal getTotalRevenueByOrganization(@Param("organization") Organization organization);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true AND t.txnDate >= :startDate")
    java.math.BigDecimal getRevenueByOrganizationAndDateAfter(@Param("organization") Organization organization, @Param("startDate") Date startDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.client.organization = :organization AND t.settled = true AND t.txnDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getRevenueByOrganizationAndDateBetween(@Param("organization") Organization organization,
                                                                @Param("startDate") Date startDate,
                                                                @Param("endDate") Date endDate);

    // Daily transaction trends
    @Query("SELECT DATE(t.txnDate) as transactionDate, COUNT(t) as transactionCount, SUM(t.amount) as totalAmount " +
            "FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(t.txnDate) " +
            "ORDER BY transactionDate ASC")
    List<Object[]> getDailyTransactionTrend(@Param("organization") Organization organization,
                                            @Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    // Monthly transaction trends
    @Query("SELECT YEAR(t.txnDate) as year, MONTH(t.txnDate) as month, COUNT(t) as transactionCount, SUM(t.amount) as totalAmount " +
            "FROM Transaction t WHERE t.client.organization = :organization AND t.txnDate BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(t.txnDate), MONTH(t.txnDate) " +
            "ORDER BY year ASC, month ASC")
    List<Object[]> getMonthlyTransactionTrend(@Param("organization") Organization organization,
                                              @Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);
}
