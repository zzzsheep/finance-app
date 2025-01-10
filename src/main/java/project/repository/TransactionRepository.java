// project/repository/TransactionRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.model.Transaction;
import project.model.TransactionCategory;

import java.time.LocalDateTime;
import java.util.List;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Find transactions by date range
    List<Transaction> findByAccountIdAndTransactionDateBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    // Find by Id
    List<Transaction> findByAccountId(Long accountId);

    // Find by user email and date range
    List<Transaction> findByAccountUserEmailAndTransactionDateBetween(
            String userEmail, LocalDateTime startDate, LocalDateTime endDate
    );  // Added missing semicolon

    // Find by category (using enum)
    List<Transaction> findByCategory(TransactionCategory category);  // Changed to enum

    List<Transaction> findByAccountIdAndCategory(Long accountId, TransactionCategory category);

    // Find by merchant
    List<Transaction> findByMerchantContainingIgnoreCase(String merchant);

    // Date-based queries
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    List<Transaction> findByTransactionDateAfter(LocalDateTime date);

    // Category totals
    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.transactionDate >= :startDate " +
            "GROUP BY t.category")
    List<Object[]> findCategoryTotals(@Param("startDate") LocalDateTime startDate);

    // Removed duplicate sumByCategory as it's similar to findCategoryTotals
}








