// project/security/JwtProperties.java
package project.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;  // Add this

@Component
@ConfigurationProperties(prefix = "application.jwt")
@Data  // Add this to automatically generate getters and setters
public class JwtProperties {
    private String secretKey;
    private long expiration;
    private String tokenPrefix;
}