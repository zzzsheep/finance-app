package project.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")

public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private BigDecimal amount;
    private String description;
    private String merchant;
    private String category;  //store catergory thats provided by bank
    private String subCategory; // For more detailed categories

    private LocalDateTime transactionDate;
    private String transactionId;   //Original transaction ID from bank
    private Boolean pending;    // Transaction status

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
