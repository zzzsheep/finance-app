package project.model;

import jakarta.persistence.Entity;

// project/model/Role.java
public enum Role {
    USER,
    ADMIN
}

// Update User.java to include role
@Entity
public class User {
    // ... existing fields
    @Enumerated(EnumType.STRING)
    private Role role;
}
