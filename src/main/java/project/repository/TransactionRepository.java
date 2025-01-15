// project/repository/TransactionRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.model.Account;
import project.model.Transaction;
import project.model.TransactionCategory;
import project.model.TransactionType;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Find transactions by date range
    List<Transaction> findByAccountIdAndTransactionDateBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    // Find by account list
    List<Transaction> findByAccountIn(List<Account> accounts);

    // Find by account list and date range
    List<Transaction> findByAccountInAndTransactionDateBetween(
            List<Account> accounts,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Find by account list and type
    List<Transaction> findByAccountInAndType(
            List<Account> accounts,
            TransactionType type
    );

    // Find by Id
    List<Transaction> findByAccountId(Long accountId);

    // Find by user email and date range
    List<Transaction> findByAccountUserEmailAndTransactionDateBetween(
            String userEmail, LocalDateTime startDate, LocalDateTime endDate
    );  // Added missing semicolon

    // Find by account list and category
    List<Transaction> findByAccountInAndCategory(
            List<Account> accounts,
            TransactionCategory category
    );

    // Find by category (using enum)
    List<Transaction> findByCategory(TransactionCategory category);  // Changed to enum

    List<Transaction> findByAccountIdAndCategory(Long accountId, TransactionCategory category);

    // Find by merchant
    List<Transaction> findByMerchantContainingIgnoreCase(String merchant);

    // Date-based queries
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    List<Transaction> findByTransactionDateAfter(LocalDateTime date);

    // Find pending transactions for accounts
    List<Transaction> findByAccountInAndPendingTrue(List<Account> accounts);

    // Additional useful queries
    @Query("SELECT t FROM Transaction t WHERE t.account IN :accounts " +
            "AND t.transactionDate >= :startDate " +
            "ORDER BY t.amount DESC")
    List<Transaction> findLargeTransactions(
            @Param("accounts") List<Account> accounts,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );

    // Category totals
    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.transactionDate >= :startDate " +
            "GROUP BY t.category")
    List<Object[]> findCategoryTotals(@Param("startDate") LocalDateTime startDate);

    List<Transaction> findByAccountUserEmailAndPendingTrue(String userEmail);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.account.user.email = :userEmail " +
            "AND t.transactionDate >= :startDate " +
            "AND t.type = :type " +
            "ORDER BY t.amount DESC")
    List<Transaction> findLargeTransactions(
            @Param("userEmail") String userEmail,
            @Param("startDate") LocalDateTime startDate,
            @Param("type") TransactionType type,
            Pageable pageable
    );
    // Removed duplicate sumByCategory as it's similar to findCategoryTotals
}








