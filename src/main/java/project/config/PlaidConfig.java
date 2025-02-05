package project.config;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;

@Configuration
public class PlaidConfig {
    @Value("${plaid.clientId}")
    private String clientId;

    @Value("${plaid.secret}")
    private String secret;

    @Value("${plaid.environment}")
    private String environment;

    @Bean
    public PlaidApi plaidApi() {
        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);

        ApiClient apiClient = new ApiClient(apiKeys);

        // Set environment
        switch (environment.toLowerCase()) {
            case "sandbox":
                apiClient.setPlaidAdapter(ApiClient.Sandbox);
                break;
            case "production":
                apiClient.setPlaidAdapter(ApiClient.Production);
                break;
            default:
                throw new IllegalStateException("Invalid Plaid environment: " + environment);
        }

        return apiClient.createService(PlaidApi.class);
    }
}