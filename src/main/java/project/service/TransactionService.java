// project/service/TransactionService.java
package project.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dto.FinancialSummary;
import project.dto.TransactionDTO;
import project.model.Transaction;
import project.model.Account;
import project.model.TransactionCategory;
import project.repository.TransactionRepository;
import project.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import project.model.TransactionType;
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Transaction createTransaction(String userEmail, TransactionDTO dto) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // Verify account ownership
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Not authorized to access this account");
        }

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setCategory(dto.getCategory());
        transaction.setDescription(dto.getDescription());
        transaction.setMerchant(dto.getMerchant());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Update account balance
        updateAccountBalance(account, transaction);

        return transactionRepository.save(transaction);
    }

    private void updateAccountBalance(Account account, Transaction transaction) {
        switch (transaction.getType()) {
            case INCOME:
                account.setBalance(account.getBalance().add(transaction.getAmount()));
                break;
            case EXPENSE:
                account.setBalance(account.getBalance().subtract(transaction.getAmount()));
                break;
            case TRANSFER:
                // Handle transfer logic
                break;
        }
        accountRepository.save(account);
    }

    // Add methods for financial analysis
    public FinancialSummary getFinancialSummary(String userEmail, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findByAccountUserEmailAndTransactionDateBetween(
                userEmail, startDate, endDate);

        FinancialSummary summary = new FinancialSummary();

        // Calculate totals
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate category breakdown
        Map<TransactionCategory, BigDecimal> categoryBreakdown = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        summary.setTotalIncome(totalIncome);
        summary.setTotalExpenses(totalExpenses);
        summary.setTotalBalance(totalIncome.subtract(totalExpenses));
        summary.setCategoryBreakdown(categoryBreakdown);

        return summary;
    }
}