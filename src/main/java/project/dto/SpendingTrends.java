package project.dto;

import lombok.Data;
import project.model.TransactionCategory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@Data
public class SpendingTrends {
    private Map<YearMonth, BigDecimal> monthlySpending;
    private Map<TransactionCategory, BigDecimal> categoryTrends;
    private Map<String, Double> percentageChanges;
}
