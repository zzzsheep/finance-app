// project/dto/TransactionDTO.java
package project.dto;

import lombok.Data;
import project.model.TransactionType;
import project.model.TransactionCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionCategory category;
    private String description;
    private String merchant;
    private LocalDateTime transactionDate;
    private Long targetAccountId;  // For transfers
}