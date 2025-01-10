package project.dto;

import lombok.Data;
import project.model.TransactionCategory;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class MonthlyAnalysis {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private Map<TransactionCategory, BigDecimal> spendingByCategory;
    private Map<String, BigDecimal> topMerchants;
}