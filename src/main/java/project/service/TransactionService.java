// project/service/TransactionService.java
package project.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dto.FinancialSummary;
import project.dto.MonthlyAnalysis;
import project.dto.SpendingTrends;
import project.dto.TransactionDTO;
import project.model.Transaction;
import project.model.Account;
import project.model.TransactionCategory;
import project.repository.TransactionRepository;
import project.repository.AccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import project.model.TransactionType;
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionValidationService validationService;
    private final AccountService accountService;
    @Transactional
    public Transaction createTransaction(String userEmail, TransactionDTO dto) {

        // Validate input
        validationService.validateTransactionDTO(dto);
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // Verify account ownership
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Not authorized to access this account");
        }
        // Handle transfer transactions
        if (dto.getType() == TransactionType.TRANSFER) {
            return handleTransferTransaction(userEmail, dto, account);
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
        transaction.setPending(true); // Set initial pending status
        // Validate sufficient funds for expenses
        if (dto.getType() == TransactionType.EXPENSE) {
            validateSufficientFunds(account, dto.getAmount());
        }
        // Update account balance
        updateAccountBalance(account, transaction);

        return transactionRepository.save(transaction);
    }

    @Transactional
    private Transaction handleTransferTransaction(String userEmail, TransactionDTO dto, Account sourceAccount) {
        if (dto.getTargetAccountId() == null) {
            throw new IllegalArgumentException("Target account is required for transfers");
        }

        Account targetAccount = accountRepository.findById(dto.getTargetAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Target account not found"));

        // Create source transaction (withdrawal)
        Transaction sourceTransaction = new Transaction();
        sourceTransaction.setAccount(sourceAccount);
        sourceTransaction.setAmount(dto.getAmount().negate());
        sourceTransaction.setType(TransactionType.TRANSFER);
        sourceTransaction.setDescription("Transfer to account " + targetAccount.getId());
        sourceTransaction.setTransactionDate(dto.getTransactionDate());
        sourceTransaction.setCreatedAt(LocalDateTime.now());
        sourceTransaction.setPending(true);

        // Create target transaction (deposit)
        Transaction targetTransaction = new Transaction();
        targetTransaction.setAccount(targetAccount);
        targetTransaction.setAmount(dto.getAmount());
        targetTransaction.setType(TransactionType.TRANSFER);
        targetTransaction.setDescription("Transfer from account " + sourceAccount.getId());
        targetTransaction.setTransactionDate(dto.getTransactionDate());
        targetTransaction.setCreatedAt(LocalDateTime.now());
        targetTransaction.setPending(true);

        // Update both account balances
        validateSufficientFunds(sourceAccount, dto.getAmount());
        updateAccountBalance(sourceAccount, sourceTransaction);
        updateAccountBalance(targetAccount, targetTransaction);

        transactionRepository.save(targetTransaction);
        return transactionRepository.save(sourceTransaction);
    }

    private void validateSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account");
        }
    }

    // Enhanced account balance update with validation
    private void updateAccountBalance(Account account, Transaction transaction) {
        BigDecimal newBalance;
        switch (transaction.getType()) {
            case INCOME:
                newBalance = account.getBalance().add(transaction.getAmount());
                break;
            case EXPENSE:
            case TRANSFER:
                newBalance = account.getBalance().subtract(transaction.getAmount().abs());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InsufficientFundsException("Transaction would result in negative balance");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid transaction type");
        }
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    // Get pending transactions
    public List<Transaction> getPendingTransactions(String userEmail) {
        return transactionRepository.findByAccountUserEmailAndPendingTrue(userEmail);
    }

    // Get all transactions for a user
    public List<Transaction> getUserTransactions(String userEmail) {
        // Get all accounts for the user
        List<Account> userAccounts = accountRepository.findByUserEmail(userEmail);

        // Get transactions for all accounts
        return transactionRepository.findByAccountIn(userAccounts);
    }


    //getTransactionById
    public Transaction getTransactionById(String userEmail, Long transactionId){
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        // Security check - verify the transaction belongs to the user
        if (!transaction.getAccount().getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Not authorized to access this transaction");
        }

        return transaction;
    }
    // Confirm transaction (mark as not pending)
    @Transactional
    public Transaction confirmTransaction(String userEmail, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        if (!transaction.getAccount().getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Not authorized to access this transaction");
        }

        transaction.setPending(false);
        transaction.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    // Get filtered transactions for a user
    public List<Transaction> getUserTransactionsFiltered(String userEmail,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         TransactionType type,
                                                         TransactionCategory category) {
        List<Account> userAccounts = accountRepository.findByUserEmail(userEmail);

        if (startDate != null && endDate != null) {
            return transactionRepository.findByAccountInAndTransactionDateBetween(
                    userAccounts, startDate, endDate);
        } else if (type != null) {
            return transactionRepository.findByAccountInAndType(userAccounts, type);
        } else if (category != null) {
            return transactionRepository.findByAccountInAndCategory(userAccounts, category);
        }

        return transactionRepository.findByAccountIn(userAccounts);
    }

    public FinancialSummary getFinancialSummary(String userEmail) {
        List<Account> userAccounts = accountRepository.findByUserEmail(userEmail);
        List<Transaction> allTransactions = transactionRepository.findByAccountIn(userAccounts);

        FinancialSummary summary = new FinancialSummary();

        // Calculate total balance across all accounts
        BigDecimal totalBalance = userAccounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate income and expenses
        BigDecimal totalIncome = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate category breakdown
        Map<TransactionCategory, BigDecimal> categoryBreakdown = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        // Generate insights
        List<String> insights = generateInsights(allTransactions, categoryBreakdown);

        summary.setTotalBalance(totalBalance);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpenses(totalExpenses);
        summary.setCategoryBreakdown(categoryBreakdown);
        summary.setInsights(insights);

        return summary;
    }

    public MonthlyAnalysis getMonthlyAnalysis(String userEmail, YearMonth yearMonth) {
        List<Account> userAccounts = accountRepository.findByUserEmail(userEmail);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> monthlyTransactions = transactionRepository
                .findByAccountInAndTransactionDateBetween(userAccounts, startOfMonth, endOfMonth);

        MonthlyAnalysis analysis = new MonthlyAnalysis();

        // Calculate monthly totals
        BigDecimal monthlyIncome = monthlyTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyExpenses = monthlyTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate spending by category
        Map<TransactionCategory, BigDecimal> spendingByCategory = monthlyTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        // Calculate top merchants
        Map<String, BigDecimal> topMerchants = monthlyTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getMerchant() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getMerchant,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        analysis.setTotalIncome(monthlyIncome);
        analysis.setTotalExpenses(monthlyExpenses);
        analysis.setNetSavings(monthlyIncome.subtract(monthlyExpenses));
        analysis.setSpendingByCategory(spendingByCategory);
        analysis.setTopMerchants(topMerchants);

        return analysis;
    }

    public SpendingTrends getSpendingTrends(String userEmail) {
        List<Account> userAccounts = accountRepository.findByUserEmail(userEmail);
        List<Transaction> allTransactions = transactionRepository.findByAccountIn(userAccounts);

        SpendingTrends trends = new SpendingTrends();

        // Calculate monthly spending
        Map<YearMonth, BigDecimal> monthlySpending = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate()),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        // Calculate category trends
        Map<TransactionCategory, BigDecimal> categoryTrends = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        // Calculate percentage changes
        Map<String, Double> percentageChanges = calculatePercentageChanges(monthlySpending);

        trends.setMonthlySpending(monthlySpending);
        trends.setCategoryTrends(categoryTrends);
        trends.setPercentageChanges(percentageChanges);

        return trends;
    }

    private List<String> generateInsights(List<Transaction> transactions,
                                          Map<TransactionCategory, BigDecimal> categoryBreakdown) {
        List<String> insights = new ArrayList<>();

        // Add insights based on spending patterns
        // Example insights:
        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthSpending = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> YearMonth.from(t.getTransactionDate()).equals(currentMonth))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add monthly spending insight
        insights.add("Your total spending this month is " + currentMonthSpending);

        // Add category-based insights
        categoryBreakdown.forEach((category, amount) -> {
            if (amount.compareTo(new BigDecimal("1000")) > 0) {
                insights.add("High spending in " + category + ": " + amount);
            }
        });

        return insights;
    }

    private Map<String, Double> calculatePercentageChanges(Map<YearMonth, BigDecimal> monthlySpending) {
        Map<String, Double> changes = new HashMap<>();

        // Calculate month-over-month changes
        List<YearMonth> sortedMonths = new ArrayList<>(monthlySpending.keySet());
        Collections.sort(sortedMonths);

        for (int i = 1; i < sortedMonths.size(); i++) {
            YearMonth currentMonth = sortedMonths.get(i);
            YearMonth previousMonth = sortedMonths.get(i - 1);

            BigDecimal currentAmount = monthlySpending.get(currentMonth);
            BigDecimal previousAmount = monthlySpending.get(previousMonth);

            double percentageChange = previousAmount.equals(BigDecimal.ZERO) ? 0 :
                    currentAmount.subtract(previousAmount)
                            .divide(previousAmount, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .doubleValue();

            changes.put(currentMonth.toString(), percentageChange);
        }

        return changes;
    }
}
//now I have these test scripts can you check if they are perfectly fine, or need fixing, for me to test the transaction system and the analysis parts as well.