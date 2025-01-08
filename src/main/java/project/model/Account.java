package project.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity // Database table
@Data // Lombok annotation that auto create getters, setters, toString, etc.
@Table(name = "accounts") // Names the table in the database
public class Account {
    // Primary key that auto-increments
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private long id;

    // Many accounts can belong to one user

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Creates a relationhip between User and account tables.
    @JsonManagedReference
    private User user;

    // basic account information
    private String accountNumber;
    private String accountName;

    //uses the AccountType enum to limit other possible values
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    // BigDecimal is used for money thats more precise than double
    private BigDecimal balance;

    private String bankName;

    //Create TimeStamps that can't be changed after creation
    @Column(updatable = false)
    private LocalDateTime createdAt;

    //Last update timestamp
    private LocalDateTime updatedAt;
}
