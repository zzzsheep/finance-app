package project.dto;

import lombok.Data;
import project.model.TransactionCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class FinancialSummary {
    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Map<TransactionCategory, BigDecimal> categoryBreakdown;
    private List<String> insights;
}