// project/repository/TransactionRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // Find by category
    List<Transaction> findByCategory(String category);

    List<Transaction> findByAccountIdAndCategory(Long accountId, TransactionCategory category);

    // Find by merchant
    List<Transaction> findByMerchantContainingIgnoreCase(String merchant);

    // Group transactions by category
    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :start AND :end " +
            "GROUP BY t.category")
    List<Object[]> sumByCategory(LocalDateTime start, LocalDateTime end);
}


