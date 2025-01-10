package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.model.Transaction;
import project.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;

import project.dto.*;
import project.model.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisService {
    private final TransactionRepository transactionRepository;


    public MonthlyAnalysis getMonthlyAnalysis(YearMonth month) {
        // Convert YearMonth to LocalDateTime range
        LocalDateTime startDate = month.atDay(1).atStartOfDay();
        LocalDateTime endDate = month.atEndOfMonth().atTime(23, 59, 59);

        // Get transactions for the month
        List<Transaction> transactions = transactionRepository
                .findByTransactionDateBetween(startDate, endDate);

        MonthlyAnalysis analysis = new MonthlyAnalysis();

        // Calculate totals
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by category
        Map<TransactionCategory, BigDecimal> spendingByCategory = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        // Get top merchants
        Map<String, BigDecimal> topMerchants = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getMerchant,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));

        analysis.setTotalIncome(totalIncome);
        analysis.setTotalExpenses(totalExpenses);
        analysis.setNetSavings(totalIncome.subtract(totalExpenses));
        analysis.setSpendingByCategory(spendingByCategory);
        analysis.setTopMerchants(topMerchants);

        return analysis;
    }

    public SpendingTrends getSpendingTrends() {
        // Get last 6 months of data
        YearMonth currentMonth = YearMonth.now();
        Map<YearMonth, BigDecimal> monthlySpending = new TreeMap<>();

        for (int i = 0; i < 6; i++) {
            YearMonth month = currentMonth.minusMonths(i);
            MonthlyAnalysis monthAnalysis = getMonthlyAnalysis(month);
            monthlySpending.put(month, monthAnalysis.getTotalExpenses());
        }

        // Calculate category trends
        Map<TransactionCategory, BigDecimal> categoryTrends = getCategoryTrends();

        // Calculate percentage changes
        Map<String, Double> percentageChanges = calculatePercentageChanges(monthlySpending);

        SpendingTrends trends = new SpendingTrends();
        trends.setMonthlySpending(monthlySpending);
        trends.setCategoryTrends(categoryTrends);
        trends.setPercentageChanges(percentageChanges);

        return trends;
    }

    private Map<TransactionCategory, BigDecimal> getCategoryTrends() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        return transactionRepository.findByTransactionDateAfter(sixMonthsAgo)
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));
    }

    private Map<String, Double> calculatePercentageChanges(
            Map<YearMonth, BigDecimal> monthlySpending) {
        Map<String, Double> changes = new HashMap<>();
        List<YearMonth> months = new ArrayList<>(monthlySpending.keySet());

        if (months.size() >= 2) {
            YearMonth currentMonth = months.get(0);
            YearMonth previousMonth = months.get(1);
            BigDecimal currentAmount = monthlySpending.get(currentMonth);
            BigDecimal previousAmount = monthlySpending.get(previousMonth);

            if (previousAmount.compareTo(BigDecimal.ZERO) != 0) {
                double percentageChange = currentAmount
                        .subtract(previousAmount)
                        .divide(previousAmount, 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100))
                        .doubleValue();

                changes.put("month_over_month", percentageChange);
            }
        }

        return changes;
    }

}
