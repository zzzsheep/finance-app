// src/main/java/project/financeapp/repository/UserRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);
}