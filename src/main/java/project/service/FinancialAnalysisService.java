package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.model.Transaction;
import project.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisService {
    private final TransactionRepository transactionRepository;

    public Map<String, BigDecimal> getSpendingByCategory(
            LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = transactionRepository
                .sumByCategory(startDate, endDate);

        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Object[] result : results) {
            categoryTotals.put(
                    (String) result[0],
                    (BigDecimal) result[1]
            );
        }
        return categoryTotals;
    }

    public List<Transaction> getTopMerchants(int limit) {
        // Implementation to find most frequent merchants
    }

    public MonthlyAnalysis getMonthlyAnalysis(YearMonth month) {
        // Monthly spending analysis
    }

    public SpendingTrends getSpendingTrends() {
        // Analyze spending trends over time
    }
}
